package com.ruoyi.web.controller.library;

import com.ruoyi.library.domain.WlQrConfig;
import com.ruoyi.library.dto.QrConfigView;
import com.ruoyi.library.service.QrConfigService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LibraryQrConfigControllerTest
{
    @TempDir
    Path root;

    @Test
    void exposesManagementListAndImageOperations() throws Exception
    {
        QrConfigService service = mock(QrConfigService.class);
        when(service.list(any())).thenReturn(Arrays.asList(
                new QrConfigView(5L, "加入社群", "长按识别", 10, "0", true,
                        "/wx/qr-configs/5/image")));
        when(service.uploadImage(eq(5L), any(), eq("admin"))).thenReturn(1);
        when(service.clearImage(5L, "admin")).thenReturn(1);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController(service)).build();

        mockMvc.perform(get("/library/qr-config/list"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].menuName").value("加入社群"))
                .andExpect(jsonPath("$.rows[0].imageConfigured").value(true));

        mockMvc.perform(multipart("/library/qr-config/5/image")
                        .file(new MockMultipartFile("image", "qr.png", "image/png", new byte[] {1})))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
        verify(service).uploadImage(eq(5L), any(), eq("admin"));

        mockMvc.perform(delete("/library/qr-config/5/image"))
                .andExpect(status().isOk());
        verify(service).clearImage(5L, "admin");
    }

    @Test
    void streamsManagementImageWithCorrectContentType() throws Exception
    {
        QrConfigService service = mock(QrConfigService.class);
        Path image = root.resolve("qr.webp");
        Files.write(image, new byte[] {1, 2, 3});
        when(service.resolveImageForManagement(5L)).thenReturn(image);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new TestController(service)).build();

        mockMvc.perform(get("/library/qr-config/5/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/webp"))
                .andExpect(content().bytes(new byte[] {1, 2, 3}));
    }

    private static final class TestController extends LibraryQrConfigController
    {
        private TestController(QrConfigService service)
        {
            super(service);
        }

        @Override
        public String getUsername()
        {
            return "admin";
        }
    }
}
