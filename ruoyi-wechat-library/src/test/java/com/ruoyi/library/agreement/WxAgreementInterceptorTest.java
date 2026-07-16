package com.ruoyi.library.agreement;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.auth.WxAuthInterceptor;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WxAgreementInterceptorTest
{
    @AfterEach
    void tearDown()
    {
        WxUserContext.clear();
    }

    @Test
    void returnsConflictWhenCurrentVersionsAreMissing() throws Exception
    {
        WxAgreementService service = mock(WxAgreementService.class);
        when(service.hasAcceptedAllCurrent(12L)).thenReturn(false);
        WxUserContext.set(12L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(WxAuthInterceptor.TOKEN_HEADER, "valid-token");
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(new WxAgreementInterceptor(service).preHandle(
                request, response, new Object()));

        assertEquals(409, response.getStatus());
        JSONObject body = JSON.parseObject(response.getContentAsString());
        assertEquals(40901, body.getIntValue("code"));
        assertEquals("请先阅读并同意最新用户隐私协议与网站声明", body.getString("message"));
    }

    @Test
    void allowsRequestAfterBothCurrentVersionsAreAccepted() throws Exception
    {
        WxAgreementService service = mock(WxAgreementService.class);
        when(service.hasAcceptedAllCurrent(12L)).thenReturn(true);
        WxUserContext.set(12L);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader(WxAuthInterceptor.TOKEN_HEADER, "valid-token");
        assertTrue(new WxAgreementInterceptor(service).preHandle(
                request, new MockHttpServletResponse(), new Object()));
    }
}
