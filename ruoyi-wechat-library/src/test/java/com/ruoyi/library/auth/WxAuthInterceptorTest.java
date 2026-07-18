package com.ruoyi.library.auth;

import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.library.common.WxUserContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

class WxAuthInterceptorTest
{
    private WxTokenService tokenService;
    private WxUserAccessService userAccessService;
    private WxAuthInterceptor interceptor;

    @BeforeEach
    void setUp()
    {
        tokenService = mock(WxTokenService.class);
        userAccessService = mock(WxUserAccessService.class);
        interceptor = new WxAuthInterceptor(tokenService, userAccessService);
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
        when(tokenService.resolveWithoutRefresh(null)).thenReturn(null);

        assertFalse(interceptor.preHandle(new MockHttpServletRequest(), response, new Object()));
        assertEquals(401, response.getStatus());
        assertEquals("application/json;charset=UTF-8", response.getContentType());
        JSONObject body = JSON.parseObject(response.getContentAsString());
        assertEquals(40101, body.getIntValue("code"));
        assertEquals("登录状态已失效，请重新登录", body.getString("message"));
        assertTrue(body.containsKey("data"));
        assertNull(body.get("data"));
    }

    @Test
    void supportsAsynchronousRequestCleanup()
    {
        assertTrue(interceptor instanceof AsyncHandlerInterceptor);
    }

    @Test
    void asynchronousHandoffClearsOriginalServletThreadContext()
            throws Exception
    {
        WxUserContext.set(99L);

        interceptor.afterConcurrentHandlingStarted(new MockHttpServletRequest(),
                new MockHttpServletResponse(), new Object());

        assertNull(WxUserContext.get());
    }

    @Test
    void validWechatTokenSetsOnlyWechatUserContext()
            throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Wx-Token", "valid-token");
        when(tokenService.resolveWithoutRefresh("valid-token")).thenReturn(99L);
        when(userAccessService.isEnabled(99L)).thenReturn(true);

        assertTrue(interceptor.preHandle(request, new MockHttpServletResponse(), new Object()));
        assertEquals(99L, WxUserContext.get());
        verify(tokenService).refresh("valid-token");
    }

    @Test
    void disabledUserIsRejectedWithoutRefreshingToken() throws Exception
    {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Wx-Token", "disabled-token");
        when(tokenService.resolveWithoutRefresh("disabled-token")).thenReturn(66L);
        when(userAccessService.isEnabled(66L)).thenReturn(false);
        MockHttpServletResponse response = new MockHttpServletResponse();

        assertFalse(interceptor.preHandle(request, response, new Object()));

        assertEquals(403, response.getStatus());
        JSONObject body = JSON.parseObject(response.getContentAsString());
        assertEquals(40301, body.getIntValue("code"));
        assertEquals("该微信用户已被停用，无法访问", body.getString("message"));
        verify(tokenService, never()).refresh("disabled-token");
        verify(tokenService).revoke("disabled-token");
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
