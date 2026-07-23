package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.DocumentConversionProperties;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.dto.DocumentUploadCommitRequest;
import com.ruoyi.library.dto.DocumentUploadCommitResult;
import com.ruoyi.library.dto.DocumentUploadPrepareResult;
import com.ruoyi.library.dto.DocumentThumbnailResult;
import com.ruoyi.library.storage.CosPrivateStorageService;
import com.ruoyi.library.storage.PreparedDocumentProcessor;
import java.io.ByteArrayOutputStream;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.nio.file.attribute.FileTime;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import javax.imageio.ImageIO;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.when;

class DocumentUploadServiceTest
{
    @TempDir
    Path tempDir;

    private CosPrivateStorageService storage;
    private DocumentService documentService;
    private DocumentUploadService service;

    @BeforeEach
    void setUp()
    {
        DocumentConversionProperties properties = new DocumentConversionProperties();
        properties.setTempDirectory(tempDir.toString());
        properties.setMaxInputBytes(10L * 1024L * 1024L);
        properties.setMaxOutputBytes(10L * 1024L * 1024L);
        storage = mock(CosPrivateStorageService.class);
        documentService = mock(DocumentService.class);
        service = new DocumentUploadService(new PreparedDocumentProcessor(properties), storage,
                documentService, properties, Clock.fixed(Instant.parse("2026-07-22T08:00:00Z"),
                ZoneId.of("Asia/Shanghai")));
    }

    @Test
    void prepareBindsSessionToOwnerAndCalculatesPreviewPages() throws Exception
    {
        DocumentUploadPrepareResult prepared = service.prepare(pdfFile("manual.pdf", 3), "admin");

        assertNotNull(prepared.getSessionId());
        assertEquals("PDF", prepared.getFileFormat());
        assertEquals(3, prepared.getPageCount());
        assertEquals(2, prepared.getPreviewPages());
        assertEquals("/library/document-upload/session/" + prepared.getSessionId() + "/thumbnail",
                prepared.getThumbnailUrl());
        assertEquals("临时文件无权访问", assertThrows(ServiceException.class,
                () -> service.thumbnail(prepared.getSessionId(), "other")).getMessage());
    }

    @Test
    void databaseFailureDeletesAllUploadedObjectsAndKeepsSessionForRetry() throws Exception
    {
        DocumentUploadPrepareResult prepared = service.prepare(pdfFile("manual.pdf", 2), "admin");
        when(storage.putPrivateObject(any(), any(), anyLong(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(documentService.addProcessedDocument(any(), eq("admin")))
                .thenThrow(new ServiceException("文档保存失败，请重试"));

        assertEquals("文档保存失败，请重试", assertThrows(ServiceException.class,
                () -> service.commit(prepared.getSessionId(), request(), "admin")).getMessage());

        verify(storage).deleteObjectAfterMetadataDeletion("documents/" + prepared.getSessionId() + "/original/v1.pdf");
        verify(storage).deleteObjectAfterMetadataDeletion("documents/" + prepared.getSessionId() + "/preview/v1.pdf");
        verify(storage).deleteObjectAfterMetadataDeletion("documents/" + prepared.getSessionId() + "/thumbnail/v1.jpg");
        assertNotNull(service.thumbnail(prepared.getSessionId(), "admin"));
    }

    @Test
    void secondUploadFailureCompensatesOnlyCompletedObject() throws Exception
    {
        DocumentUploadPrepareResult prepared = service.prepare(pdfFile("manual.pdf", 2), "admin");
        String originalKey = "documents/" + prepared.getSessionId() + "/original/v1.pdf";
        String previewKey = "documents/" + prepared.getSessionId() + "/preview/v1.pdf";
        when(storage.putPrivateObject(eq(originalKey), any(), anyLong(), any())).thenReturn(originalKey);
        when(storage.putPrivateObject(eq(previewKey), any(), anyLong(), any()))
                .thenThrow(new ServiceException("COS 上传失败"));

        assertEquals("文件保存失败，请重试", assertThrows(ServiceException.class,
                () -> service.commit(prepared.getSessionId(), request(), "admin")).getMessage());

        verify(storage).deleteObjectAfterMetadataDeletion(originalKey);
        verify(documentService, never()).addProcessedDocument(any(), any());
    }

    @Test
    void successfulCommitStoresOnlyOriginalPreviewAndThumbnail() throws Exception
    {
        DocumentUploadPrepareResult prepared = service.prepare(pdfFile("manual.pdf", 2), "admin");
        when(storage.putPrivateObject(any(), any(), anyLong(), any())).thenAnswer(invocation -> invocation.getArgument(0));
        doAnswer(invocation -> {
            WlDocument document = invocation.getArgument(0);
            document.setId(91L);
            return 1;
        }).when(documentService).addProcessedDocument(any(), eq("admin"));
        when(storage.signGetUrl(any(), any(), any())).thenReturn(new java.net.URL("https://example.test/cover"));

        DocumentUploadCommitResult result = service.commit(prepared.getSessionId(), request(), "admin");

        assertEquals(91L, result.getDocumentId());
        assertEquals("SUCCESS", result.getConversionStatus());
        verify(documentService).addProcessedDocument(any(), eq("admin"));
        assertEquals("临时文件不存在或已过期", assertThrows(ServiceException.class,
                () -> service.commit(prepared.getSessionId(), request(), "admin")).getMessage());
        verify(documentService, times(1)).addProcessedDocument(any(), eq("admin"));
        assertEquals("临时文件不存在或已过期", assertThrows(ServiceException.class,
                () -> service.thumbnail(prepared.getSessionId(), "admin")).getMessage());
    }

    @Test
    void replacementThumbnailRequiresExtensionMimeAndRealFormatToMatch() throws Exception
    {
        DocumentUploadPrepareResult prepared = service.prepare(pdfFile("manual.pdf", 2), "admin");
        MockMultipartFile disguised = imageFile("cover.png", "image/png", "jpg");

        assertEquals("缩略图扩展名、MIME 类型与实际格式不一致", assertThrows(ServiceException.class,
                () -> service.replaceThumbnail(prepared.getSessionId(), disguised, "admin")).getMessage());
    }

    @Test
    void savedDocumentThumbnailIsUpdatedBeforeOldObjectIsDeleted() throws Exception
    {
        WlDocument document = new WlDocument();
        document.setId(91L);
        document.setCoverUrl("documents/91/thumbnail/v1.jpg");
        when(documentService.getDocument(91L)).thenReturn(document);
        when(storage.putPrivateObject(any(), any(), anyLong(), eq("image/jpeg")))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.signGetUrl(any(), any(), any())).thenReturn(new java.net.URL("https://example.test/new-cover"));
        when(documentService.updateDocumentCover(eq(91L), eq("documents/91/thumbnail/v1.jpg"), any(), eq("admin")))
                .thenReturn(1);

        assertEquals("https://example.test/new-cover",
                service.replaceSavedThumbnail(91L, imageFile("cover.png", "image/png", "png"), "admin")
                        .getThumbnailUrl());

        org.mockito.InOrder order = inOrder(documentService, storage);
        order.verify(documentService).updateDocumentCover(eq(91L), eq("documents/91/thumbnail/v1.jpg"),
                any(), eq("admin"));
        order.verify(storage).deleteObjectAfterMetadataDeletion("documents/91/thumbnail/v1.jpg");
    }

    @Test
    void savedDocumentThumbnailReturnsShortLivedPrivateUrl() throws Exception
    {
        WlDocument document = new WlDocument();
        document.setId(91L);
        document.setCoverUrl("documents/91/thumbnail/v1.jpg");
        when(documentService.getDocument(91L)).thenReturn(document);
        when(storage.signGetUrl("documents/91/thumbnail/v1.jpg", Duration.ofMinutes(10), null))
                .thenReturn(new java.net.URL("https://example.test/saved-cover"));
        DocumentThumbnailResult result = service.savedThumbnail(91L, "admin");

        assertEquals(91L, result.getDocumentId());
        assertEquals("https://example.test/saved-cover", result.getThumbnailUrl());
        verify(storage).signGetUrl("documents/91/thumbnail/v1.jpg", Duration.ofMinutes(10), null);
    }

    @Test
    void savedDocumentThumbnailRejectsNonThumbnailPrivateObjectKey()
    {
        WlDocument document = new WlDocument();
        document.setId(91L);
        document.setCoverUrl("documents/91/original/v1.pdf");
        when(documentService.getDocument(91L)).thenReturn(document);

        assertEquals("文档缩略图对象键不正确", assertThrows(ServiceException.class,
                () -> service.savedThumbnail(91L, "admin")).getMessage());
    }

    @Test
    void savedDocumentWithoutThumbnailReturnsEmptyResult()
    {
        WlDocument document = new WlDocument();
        document.setId(91L);
        when(documentService.getDocument(91L)).thenReturn(document);

        DocumentThumbnailResult result = service.savedThumbnail(91L, "admin");

        assertEquals(91L, result.getDocumentId());
        assertEquals(null, result.getThumbnailUrl());
    }

    @Test
    void savedThumbnailDatabaseFailureDeletesNewObjectButPreservesOldObject() throws Exception
    {
        WlDocument document = new WlDocument();
        document.setId(91L);
        document.setCoverUrl("documents/91/thumbnail/v1.jpg");
        when(documentService.getDocument(91L)).thenReturn(document);
        when(storage.putPrivateObject(any(), any(), anyLong(), eq("image/jpeg")))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(storage.signGetUrl(any(), any(), any())).thenReturn(new java.net.URL("https://example.test/new-cover"));
        when(documentService.updateDocumentCover(eq(91L), eq("documents/91/thumbnail/v1.jpg"), any(), eq("admin")))
                .thenThrow(new ServiceException("文档缩略图已变化，请刷新后重试"));

        assertEquals("文档缩略图已变化，请刷新后重试", assertThrows(ServiceException.class,
                () -> service.replaceSavedThumbnail(91L,
                        imageFile("cover.png", "image/png", "png"), "admin")).getMessage());

        verify(storage).deleteObjectAfterMetadataDeletion(argThat(key -> key.startsWith(
                "documents/91/thumbnail/v") && !"documents/91/thumbnail/v1.jpg".equals(key)));
        verify(storage, never()).deleteObjectAfterMetadataDeletion("documents/91/thumbnail/v1.jpg");
    }

    @Test
    void startupCleanupRemovesOnlyExpiredOrphanSessionDirectory() throws Exception
    {
        Path orphan = Files.createDirectories(tempDir.resolve("upload-sessions")
                .resolve("wl-upload-0123456789abcdef0123456789abcdef"));
        Files.setLastModifiedTime(orphan, FileTime.from(Instant.parse("2026-07-22T07:00:00Z")));

        service.cleanupAfterStartup();

        assertFalse(Files.exists(orphan));
    }

    @Test
    void cancelledSessionCannotBeUsedAgain() throws Exception
    {
        DocumentUploadPrepareResult prepared = service.prepare(pdfFile("manual.pdf", 2), "admin");

        service.cancel(prepared.getSessionId(), "admin");

        assertEquals("临时文件不存在或已过期", assertThrows(ServiceException.class,
                () -> service.thumbnail(prepared.getSessionId(), "admin")).getMessage());
    }

    private DocumentUploadCommitRequest request()
    {
        DocumentUploadCommitRequest request = new DocumentUploadCommitRequest();
        request.setTitle("质量手册");
        request.setCategoryId(2L);
        request.setSummary("摘要");
        request.setTags("质量,体系");
        request.setPointPrice(10L);
        request.setSortOrder(1);
        return request;
    }

    private MockMultipartFile pdfFile(String name, int pages) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try (PDDocument pdf = new PDDocument())
        {
            for (int index = 0; index < pages; index++) pdf.addPage(new PDPage());
            pdf.save(output);
        }
        return new MockMultipartFile("file", name, "application/pdf", output.toByteArray());
    }


    private MockMultipartFile imageFile(String name, String contentType, String format) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(20, 20, BufferedImage.TYPE_INT_RGB), format, output);
        return new MockMultipartFile("file", name, contentType, output.toByteArray());
    }
}
