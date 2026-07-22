package com.ruoyi.web.controller.library;

import com.ruoyi.library.domain.WlBanner;
import com.ruoyi.library.dto.BannerImagePreviewResult;
import com.ruoyi.library.service.BannerManagementService;
import com.ruoyi.library.service.DocumentService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LibraryBannerControllerTest
{
    private BannerManagementService managementService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        managementService = mock(BannerManagementService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                new TestController(mock(DocumentService.class), managementService)).build();
    }

    @Test
    void multipartAddDeserializesBannerJsonAndImage() throws Exception
    {
        when(managementService.add(any(WlBanner.class), any(MultipartFile.class), eq("admin")))
                .thenReturn(1);

        mockMvc.perform(multipart("/library/banner")
                        .file(bannerPart("{\"title\":\"首页推荐\",\"documentId\":9}"))
                        .file(imagePart()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(managementService).add(argThat(banner ->
                        "首页推荐".equals(banner.getTitle()) && Long.valueOf(9L).equals(banner.getDocumentId())),
                any(MultipartFile.class), eq("admin"));
    }

    @Test
    void multipartPutAllowsOmittingUnchangedImage() throws Exception
    {
        when(managementService.update(any(WlBanner.class), isNull(), eq("admin"))).thenReturn(1);

        mockMvc.perform(multipart("/library/banner")
                        .file(bannerPart("{\"id\":4,\"title\":\"首页推荐\",\"documentId\":9}"))
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(managementService).update(argThat(banner -> Long.valueOf(4L).equals(banner.getId())),
                isNull(), eq("admin"));
    }

    @Test
    void imageEndpointReturnsShortLivedPreviewUrl() throws Exception
    {
        when(managementService.preview(4L)).thenReturn(
                new BannerImagePreviewResult("https://signed.example/banner.jpg"));

        mockMvc.perform(get("/library/banner/4/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.imageUrl").value("https://signed.example/banner.jpg"));
    }

    @Test
    void addRequiresImagePart() throws Exception
    {
        mockMvc.perform(multipart("/library/banner")
                        .file(bannerPart("{\"title\":\"首页推荐\",\"documentId\":9}")))
                .andExpect(status().isBadRequest());

        verify(managementService, never()).add(any(), any(), any());
    }

    @Test
    void imageEndpointDeclaresApprovedPermission() throws Exception
    {
        PreAuthorize permission = LibraryBannerController.class.getMethod("image", Long.class)
                .getAnnotation(PreAuthorize.class);

        assertEquals("@ss.hasAnyPermi('library:banner:list,library:banner:edit')", permission.value());
    }

    private MockMultipartFile bannerPart(String json)
    {
        return new MockMultipartFile("banner", "", "application/json",
                json.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile imagePart()
    {
        return new MockMultipartFile("image", "banner.jpg", "image/jpeg", new byte[] {1, 2, 3});
    }

    private static final class TestController extends LibraryBannerController
    {
        private TestController(DocumentService documentService, BannerManagementService managementService)
        {
            super(documentService, managementService);
        }

        @Override
        public String getUsername()
        {
            return "admin";
        }
    }
}
