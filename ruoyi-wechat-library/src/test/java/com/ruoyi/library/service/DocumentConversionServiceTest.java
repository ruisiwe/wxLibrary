package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.domain.WlDocumentConversion;
import com.ruoyi.library.mapper.WlDocumentConversionMapper;
import com.ruoyi.library.mapper.WlDocumentMapper;
import com.ruoyi.library.storage.CosPrivateStorageService;
import com.ruoyi.library.storage.DocumentConverter;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentConversionServiceTest
{
    private WlDocumentMapper documentMapper;
    private WlDocumentConversionMapper conversionMapper;
    private CosPrivateStorageService storage;
    private DocumentConverter converter;
    private DocumentService documentService;
    private DocumentConversionService service;

    @TempDir
    Path tempDir;

    @BeforeEach
    void setUp()
    {
        documentMapper = mock(WlDocumentMapper.class);
        conversionMapper = mock(WlDocumentConversionMapper.class);
        storage = mock(CosPrivateStorageService.class);
        converter = mock(DocumentConverter.class);
        documentService = mock(DocumentService.class);
        when(documentMapper.updateConversionStarted(any(), any())).thenReturn(1);
        when(documentMapper.updateConversionFailed(any(), any())).thenReturn(1);
        service = new DocumentConversionService(documentMapper, conversionMapper, storage,
                converter, documentService, 20L * 1024L * 1024L);
    }

    @Test
    void acceptsOnlyApprovedFormats()
    {
        assertTrue(service.supports("report.pdf"));
        assertTrue(service.supports("report.doc"));
        assertTrue(service.supports("report.docx"));
        assertTrue(service.supports("slides.ppt"));
        assertTrue(service.supports("slides.pptx"));
        assertTrue(service.supports("notes.txt"));
        assertTrue(service.supports("sheet.xls"));
        assertFalse(service.supports("sheet.xlsx"));
        assertFalse(service.supports("macro.xlsm"));
        assertFalse(service.supports("report.docx.exe"));
    }

    @Test
    void rejectsForgedOfficeUploadBeforeStorage()
    {
        WlDocument document = document("DOCX", "PENDING");
        when(documentMapper.selectDocumentById(77L)).thenReturn(document);
        MockMultipartFile forged = new MockMultipartFile("file", "report.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "not-a-zip-document".getBytes(StandardCharsets.UTF_8));

        assertEquals("文件内容与文档格式不匹配", assertThrows(ServiceException.class,
                () -> service.uploadOriginal(77L, forged, "admin")).getMessage());
        verify(storage, never()).putPrivateObject(any(), any(), any(Long.class), any());
    }

    @Test
    void successfulConversionStoresPrivatePdfKeysAndMarksSuccess() throws Exception
    {
        WlDocument document = document("DOCX", "PENDING");
        document.setPreviewPages(2);
        WlDocumentConversion task = task(9L, 77L, 1, "documents/77/original/v1.docx", "PENDING");
        when(conversionMapper.selectById(9L)).thenReturn(task);
        when(documentMapper.selectDocumentById(77L)).thenReturn(document);
        when(conversionMapper.markConverting(9L)).thenReturn(1);
        URL sourceUrl = new URL("https://temporary.example/source");
        when(storage.signGetUrl(eq(task.getSourceObjectKey()), any(), eq(null))).thenReturn(sourceUrl);
        Path full = Files.write(tempDir.resolve("full.pdf"), "%PDF-full".getBytes(StandardCharsets.UTF_8));
        Path preview = Files.write(tempDir.resolve("preview.pdf"), "%PDF-preview".getBytes(StandardCharsets.UTF_8));
        when(converter.convert(sourceUrl, "DOCX", 2, 20L * 1024L * 1024L))
                .thenReturn(new DocumentConverter.ConversionArtifacts(full, preview, 8));
        when(storage.putPrivateObject(any(), any(), any(Long.class), eq("application/pdf")))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(conversionMapper.markSuccess(eq(9L), any(), any(), eq(8))).thenReturn(1);
        when(documentMapper.updateConversionSuccess(eq(77L), any(), any(), eq(8), eq("system"))).thenReturn(1);

        WlDocumentConversion result = service.processTask(9L);

        assertEquals("SUCCESS", result.getTaskStatus());
        assertEquals(8, result.getPageCount());
        verify(storage).putPrivateObject(eq("documents/77/full/v1.pdf"), any(), any(Long.class), eq("application/pdf"));
        verify(storage).putPrivateObject(eq("documents/77/preview/v1.pdf"), any(), any(Long.class), eq("application/pdf"));
        verify(conversionMapper).markSuccess(9L, "documents/77/full/v1.pdf",
                "documents/77/preview/v1.pdf", 8);
        verify(documentMapper).updateConversionStarted(77L, "system");
    }

    @Test
    void conversionFailureIsPersistedAndCannotPublish() throws Exception
    {
        WlDocument document = document("DOCX", "FAILED");
        WlDocumentConversion task = task(9L, 77L, 1, "documents/77/original/v1.docx", "PENDING");
        when(conversionMapper.selectById(9L)).thenReturn(task);
        when(documentMapper.selectDocumentById(77L)).thenReturn(document);
        when(conversionMapper.markConverting(9L)).thenReturn(1);
        when(storage.signGetUrl(any(), any(), any())).thenReturn(new URL("https://temporary.example/source"));
        when(converter.convert(any(), eq("DOCX"), eq(2), any(Long.class)))
                .thenThrow(new ServiceException("转换进程超时"));
        when(conversionMapper.markFailed(eq(9L), eq("转换进程超时"))).thenReturn(1);

        WlDocumentConversion result = service.processTask(9L);

        assertEquals("FAILED", result.getTaskStatus());
        assertEquals("转换进程超时", result.getFailureReason());
        verify(conversionMapper).markFailed(9L, "转换进程超时");
        assertEquals("文档转换成功后才能上架", assertThrows(ServiceException.class,
                () -> service.publishDocument(77L, "admin")).getMessage());
        verify(documentService, never()).publishDocument(any(), any());
    }

    @Test
    void failureStatePersistenceMustBeAtomic() throws Exception
    {
        WlDocument document = document("DOCX", "PENDING");
        WlDocumentConversion task = task(9L, 77L, 1, "documents/77/original/v1.docx", "PENDING");
        when(conversionMapper.selectById(9L)).thenReturn(task);
        when(documentMapper.selectDocumentById(77L)).thenReturn(document);
        when(conversionMapper.markConverting(9L)).thenReturn(1);
        when(storage.signGetUrl(any(), any(), any())).thenReturn(new URL("https://temporary.example/source"));
        when(converter.convert(any(), any(), any(Integer.class), any(Long.class)))
                .thenThrow(new ServiceException("转换进程超时"));
        when(conversionMapper.markFailed(9L, "转换进程超时")).thenReturn(1);
        when(documentMapper.updateConversionFailed(77L, "system")).thenReturn(0);

        assertEquals("文档转换失败状态保存失败，请重试", assertThrows(ServiceException.class,
                () -> service.processTask(9L)).getMessage());
    }

    private WlDocument document(String format, String conversionStatus)
    {
        WlDocument document = new WlDocument();
        document.setId(77L);
        document.setFileFormat(format);
        document.setPreviewPages(2);
        document.setPageCount(8);
        document.setConversionStatus(conversionStatus);
        document.setPublishStatus("DRAFT");
        return document;
    }

    private WlDocumentConversion task(Long id, Long documentId, int version, String sourceKey, String status)
    {
        WlDocumentConversion task = new WlDocumentConversion();
        task.setId(id);
        task.setDocumentId(documentId);
        task.setTaskVersion(version);
        task.setSourceObjectKey(sourceKey);
        task.setTaskStatus(status);
        return task;
    }
}
