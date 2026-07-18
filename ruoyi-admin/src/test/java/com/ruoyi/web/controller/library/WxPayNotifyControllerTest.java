package com.ruoyi.web.controller.library;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.service.VipOrderService;
import com.ruoyi.library.service.VipRefundService;
import com.ruoyi.web.controller.library.wx.WxApiExceptionHandler;
import com.ruoyi.web.controller.library.wx.WxPayNotifyController;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WxPayNotifyControllerTest
{
    @Test
    void paymentFailureUsesWechatPayCallbackContract() throws Exception
    {
        VipOrderService orderService = mock(VipOrderService.class);
        doThrow(new ServiceException("验签失败"))
                .when(orderService).handlePaymentNotification(any(), any());
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(
                        new WxPayNotifyController(orderService, mock(VipRefundService.class)))
                .setControllerAdvice(new WxApiExceptionHandler()).build();

        mockMvc.perform(post("/wx/pay/notify/payment")
                        .header("Wechatpay-Signature", "invalid")
                        .contentType("application/json").content("{}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("FAIL"))
                .andExpect(jsonPath("$.message").value("通知处理失败，请稍后重试"))
                .andExpect(jsonPath("$.data").doesNotExist());
    }
}
