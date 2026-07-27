package com.ruoyi.web.controller.library;

import com.ruoyi.library.dto.VipPageConfigView;
import com.ruoyi.library.service.VipPageConfigService;
import com.ruoyi.web.controller.library.wx.WxVipPageController;
import java.util.Arrays;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WxVipPageControllerTest
{
    @Test
    void returnsVipBenefitsAndCustomerServiceInformation() throws Exception
    {
        VipPageConfigService service = mock(VipPageConfigService.class);
        when(service.getPublicView()).thenReturn(new VipPageConfigView(
                Arrays.asList("赠送积分", "VIP 文档免费下载"),
                "开通 VIP 请添加客服微信",
                "https://signed.example/wechat.png"));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new WxVipPageController(service)).build();

        mockMvc.perform(get("/wx/vip/page-config"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.benefits[0]").value("赠送积分"))
                .andExpect(jsonPath("$.data.customerServiceTip").value("开通 VIP 请添加客服微信"))
                .andExpect(jsonPath("$.data.customerServiceImageUrl").value(
                        "https://signed.example/wechat.png"));
    }
}
