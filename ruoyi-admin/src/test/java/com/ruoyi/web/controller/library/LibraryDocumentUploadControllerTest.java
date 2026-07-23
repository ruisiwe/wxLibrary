package com.ruoyi.web.controller.library;

import com.ruoyi.library.dto.DocumentUploadCommitResult;
import com.ruoyi.library.dto.DocumentUploadPrepareResult;
import com.ruoyi.library.dto.DocumentThumbnailResult;
import com.ruoyi.library.service.DocumentUploadService;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LibraryDocumentUploadControllerTest
{
    @TempDir
    Path tempDir;

    private DocumentUploadService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        service = mock(DocumentUploadService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController(service)).build();
    }

    @Test
    void multipartPrepareReturnsSessionMetadata() throws Exception
    {
        DocumentUploadPrepareResult result = new DocumentUploadPrepareResult();
        result.setSessionId("0123456789abcdef0123456789abcdef");
        result.setPageCount(3);
        result.setPreviewPages(2);
        when(service.prepare(any(), eq("admin"))).thenReturn(result);
        MockMultipartFile file = new MockMultipartFile("file", "manual.pdf", "application/pdf",
                "%PDF-test".getBytes(StandardCharsets.UTF_8));

        mockMvc.perform(multipart("/library/document-upload/prepare").file(file))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sessionId").value(result.getSessionId()))
                .andExpect(jsonPath("$.data.pageCount").value(3))
                .andExpect(jsonPath("$.data.previewPages").value(2));
    }

    @Test
    void thumbnailEndpointsUseRealMultipartAndImageResponse() throws Exception
    {
        String sessionId = "0123456789abcdef0123456789abcdef";
        Path thumbnail = Files.write(tempDir.resolve("thumbnail.jpg"), new byte[]{(byte) 0xFF, (byte) 0xD8, 1});
        when(service.thumbnail(sessionId, "admin")).thenReturn(thumbnail);
        MockMultipartFile replacement = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1});

        mockMvc.perform(get("/library/document-upload/session/{sessionId}/thumbnail", sessionId))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.IMAGE_JPEG))
                .andExpect(content().bytes(Files.readAllBytes(thumbnail)));
        mockMvc.perform(multipart("/library/document-upload/session/{sessionId}/thumbnail", sessionId)
                        .file(replacement).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk());

        verify(service).replaceThumbnail(eq(sessionId), any(), eq("admin"));
    }

    @Test
    void jsonCommitAndDeleteCancellationReachBoundOwnerSession() throws Exception
    {
        String sessionId = "0123456789abcdef0123456789abcdef";
        DocumentUploadCommitResult result = new DocumentUploadCommitResult();
        result.setDocumentId(91L);
        result.setConversionStatus("SUCCESS");
        when(service.commit(eq(sessionId), any(), eq("admin"))).thenReturn(result);

        mockMvc.perform(post("/library/document-upload/session/{sessionId}/commit", sessionId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"质量手册\",\"categoryId\":2,\"pointPrice\":10}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(91L))
                .andExpect(jsonPath("$.data.conversionStatus").value("SUCCESS"));
        mockMvc.perform(delete("/library/document-upload/session/{sessionId}", sessionId))
                .andExpect(status().isOk());

        verify(service).cancel(sessionId, "admin");
    }

    @Test
    void savedDocumentThumbnailUsesRealMultipartPut() throws Exception
    {
        MockMultipartFile replacement = new MockMultipartFile("file", "cover.png", "image/png", new byte[]{1});

        mockMvc.perform(multipart("/library/document-upload/document/{documentId}/thumbnail", 91L)
                        .file(replacement).with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk());

        verify(service).replaceSavedThumbnail(eq(91L), any(), eq("admin"));
    }

    @Test
    void savedDocumentThumbnailProvidesAuthenticatedReadEndpoint() throws Exception
    {
        DocumentThumbnailResult result = new DocumentThumbnailResult();
        result.setDocumentId(91L);
        result.setThumbnailUrl("https://example.test/saved-cover");
        when(service.savedThumbnail(91L, "admin")).thenReturn(result);

        mockMvc.perform(get("/library/document-upload/document/{documentId}/thumbnail", 91L))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.documentId").value(91L))
                .andExpect(jsonPath("$.data.thumbnailUrl").value(result.getThumbnailUrl()));

        verify(service).savedThumbnail(91L, "admin");
    }

    @Test
    void endpointsDeclareChineseManagementPermissions() throws Exception
    {
        assertEquals("@ss.hasPermi('library:document:edit')",
                LibraryDocumentUploadController.class.getMethod("savedThumbnail", Long.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermi('library:document:add') and @ss.hasPermi('library:document:upload')",
                LibraryDocumentUploadController.class.getMethod("prepare", org.springframework.web.multipart.MultipartFile.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermi('library:document:add') and @ss.hasPermi('library:document:upload')",
                LibraryDocumentUploadController.class.getMethod("commit", String.class,
                        com.ruoyi.library.dto.DocumentUploadCommitRequest.class)
                        .getAnnotation(PreAuthorize.class).value());
        assertEquals("@ss.hasPermi('library:document:edit') and @ss.hasPermi('library:document:upload')",
                LibraryDocumentUploadController.class.getMethod("replaceSavedThumbnail", Long.class,
                        org.springframework.web.multipart.MultipartFile.class)
                        .getAnnotation(PreAuthorize.class).value());
    }

    private static final class TestController extends LibraryDocumentUploadController
    {
        private TestController(DocumentUploadService service) { super(service); }
        @Override public String getUsername() { return "admin"; }
    }
}
