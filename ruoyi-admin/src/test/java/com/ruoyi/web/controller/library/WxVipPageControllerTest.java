package com.ruoyi.web.controller.library;

import com.ruoyi.library.dto.VipPageConfigView;
import com.ruoyi.library.service.VipPageConfigService;
import com.ruoyi.web.controller.library.wx.WxVipPageController;
import com.ruoyi.web.controller.library.wx.WxVipPageImageController;
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

class WxVipPageControllerTest
{
    @TempDir
    Path root;

    @Test
    void returnsVipBenefitsAndLocalCustomerServiceImageUrl() throws Exception
    {
        VipPageConfigService service = mock(VipPageConfigService.class);
        when(service.getPublicView()).thenReturn(new VipPageConfigView(
                Arrays.asList("赠送积分", "VIP 文档免费下载"),
                "开通 VIP 请添加客服微信",
                "/wx/public/vip-page-config/customer-service-image"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WxVipPageController(service)).build();

        mockMvc.perform(get("/wx/vip/page-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.benefits[0]").value("赠送积分"))
                .andExpect(jsonPath("$.data.customerServiceTip").value("开通 VIP 请添加客服微信"))
                .andExpect(jsonPath("$.data.customerServiceImageUrl").value(
                        "/wx/public/vip-page-config/customer-service-image"));
    }

    @Test
    void publicControllerStreamsLocalCustomerServiceImage() throws Exception
    {
        VipPageConfigService service = mock(VipPageConfigService.class);
        Path image = root.resolve("wechat.jpg");
        Files.write(image, new byte[] {7, 8, 9});
        when(service.resolveCustomerServiceImageForRead()).thenReturn(image);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new WxVipPageImageController(service)).build();

        mockMvc.perform(get("/wx/public/vip-page-config/customer-service-image"))
                .andExpect(status().isOk())
                .andExpect(content().contentType("image/jpeg"))
                .andExpect(content().bytes(new byte[] {7, 8, 9}));
    }
}
