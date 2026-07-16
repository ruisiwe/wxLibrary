package com.ruoyi.library.auth;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.library.common.WxUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WxAuthInterceptorTest
{
    private WxTokenService tokenService;
    private WxAuthInterceptor interceptor;

    @BeforeEach
    void setUp()
    {
        tokenService = mock(WxTokenService.class);
        interceptor = new WxAuthInterceptor(tokenService);
    }

    @AfterEach
    void tearDown()
    {
        WxUserContext.clear();
    }

    @Test
    void missingTokenReturnsUnifiedChineseUnauthorizedResponse()
            throws Exception
    {
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(tokenService.resolve(null)).thenReturn(null);

        assertFalse(interceptor.preHandle(new MockHttpServletRequest(), response, new Object()));
        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        JSONObject body = JSON.parseObject(response.getContentAsString());
        assertEquals(40101, body.getIntValue("code"));
        assertEquals("登录状态已失效，请重新登录", body.getString("message"));
        assertNull(body.get("data"));
    }

    @Test
    void validWechatTokenSetsOnlyWechatUserContext()
            throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Wx-Token", "valid-token");
        when(tokenService.resolve("valid-token")).thenReturn(99L);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(99L, WxUserContext.get());
    }

    @Test
    void completionAlwaysClearsThreadLocal()
            throws Exception
    {
        WxUserContext.set(99L);

        interceptor.afterCompletion(new MockHttpServletRequest(), new MockHttpServletResponse(),
                new Object(), new RuntimeException("测试异常"));

        assertNull(WxUserContext.get());
    }
}
