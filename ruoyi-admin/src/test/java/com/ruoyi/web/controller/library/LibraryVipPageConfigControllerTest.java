package com.ruoyi.web.controller.library;

import com.ruoyi.library.domain.WlVipPageConfig;
import com.ruoyi.library.dto.VipPageConfigView;
import com.ruoyi.library.service.VipPageConfigService;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.multipart.MultipartFile;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LibraryVipPageConfigControllerTest
{
    private VipPageConfigService service;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        service = mock(VipPageConfigService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController(service)).build();
    }

    @Test
    void getReturnsManagementView() throws Exception
    {
        when(service.getManagementView()).thenReturn(
                new VipPageConfigView(null, "开通 VIP 请添加客服微信", "https://signed.example/wechat.png"));

        mockMvc.perform(get("/library/vip-page-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.customerServiceTip").value("开通 VIP 请添加客服微信"))
                .andExpect(jsonPath("$.data.customerServiceImageUrl").value(
                        "https://signed.example/wechat.png"));
    }

    @Test
    void multipartPutDeserializesConfigAndImage() throws Exception
    {
        when(service.update(any(WlVipPageConfig.class), any(MultipartFile.class), eq("admin")))
                .thenReturn(1);

        mockMvc.perform(multipart("/library/vip-page-config")
                        .file(configPart("{\"customerServiceTip\":\"开通 VIP 请添加客服微信\"}"))
                        .file(imagePart())
                        .with(request -> { request.setMethod("PUT"); return request; }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(service).update(argThat(config ->
                        "开通 VIP 请添加客服微信".equals(config.getCustomerServiceTip())),
                any(MultipartFile.class), eq("admin"));
    }

    @Test
    void deleteImageClearsLocalCustomerServiceImage() throws Exception
    {
        when(service.clearImage("admin")).thenReturn(1);

        mockMvc.perform(delete("/library/vip-page-config/image"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(service).clearImage("admin");
    }

    private MockMultipartFile configPart(String json)
    {
        return new MockMultipartFile("config", "", "application/json",
                json.getBytes(StandardCharsets.UTF_8));
    }

    private MockMultipartFile imagePart()
    {
        return new MockMultipartFile("image", "wechat.png", "image/png", new byte[] {1, 2, 3});
    }

    private static final class TestController extends LibraryVipPageConfigController
    {
        private TestController(VipPageConfigService service)
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
