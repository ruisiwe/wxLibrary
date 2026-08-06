package com.ruoyi.web.controller.library;

import com.ruoyi.library.dto.QrConfigView;
import com.ruoyi.library.service.QrConfigService;
import com.ruoyi.web.controller.library.wx.WxQrConfigController;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WxQrConfigControllerTest
{
    @TempDir
    Path root;

    @Test
    void returnsEnabledMenuAndDetail() throws Exception
    {
        QrConfigService service = mock(QrConfigService.class);
        QrConfigView view = new QrConfigView(
                5L, "加入社群", "长按识别二维码", 10, "0", false, null);
        when(service.listEnabled()).thenReturn(Arrays.asList(view));
        when(service.getEnabled(5L)).thenReturn(view);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WxQrConfigController(service)).build();

        mockMvc.perform(get("/wx/qr-configs"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].menuName").value("加入社群"));
        mockMvc.perform(get("/wx/qr-configs/5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.guideText").value("长按识别二维码"))
                .andExpect(jsonPath("$.data.imageConfigured").value(false));
    }

    @Test
    void streamsEnabledImage() throws Exception
    {
        QrConfigService service = mock(QrConfigService.class);
        Path image = root.resolve("group.png");
        Files.write(image, new byte[] {4, 5, 6});
        when(service.resolveEnabledImage(5L)).thenReturn(image);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WxQrConfigController(service)).build();

        mockMvc.perform(get("/wx/qr-configs/5/image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/png"))
                .andExpect(content().bytes(new byte[] {4, 5, 6}));
    }
}
