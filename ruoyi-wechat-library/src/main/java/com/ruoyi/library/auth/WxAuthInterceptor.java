package com.ruoyi.library.auth;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.AsyncHandlerInterceptor;

/**
 * 小程序接口登录拦截器，仅维护独立微信用户上下文。
 */
@Component
public class WxAuthInterceptor implements AsyncHandlerInterceptor
{
    public static final String TOKEN_HEADER = "Wx-Token";
    private static final int INVALID_TOKEN_CODE = 40101;
    private static final String INVALID_TOKEN_MESSAGE = "登录状态已失效，请重新登录";

    private final WxTokenService tokenService;

    public WxAuthInterceptor(WxTokenService tokenService)
    {
        this.tokenService = tokenService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException
    {
        WxUserContext.clear();
        Long userId;
        try
        {
            userId = tokenService.resolve(request.getHeader(TOKEN_HEADER));
        }
        catch (RuntimeException exception)
        {
            WxUserContext.clear();
            throw exception;
        }
        if (userId == null)
        {
            writeUnauthorized(response);
            return false;
        }
        WxUserContext.set(userId);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
            Exception exception)
    {
        WxUserContext.clear();
    }

    @Override
    public void afterConcurrentHandlingStarted(HttpServletRequest request, HttpServletResponse response,
            Object handler)
    {
        WxUserContext.clear();
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException
    {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(
                WxApiResponse.failure(INVALID_TOKEN_CODE, INVALID_TOKEN_MESSAGE), JSONWriter.Feature.WriteNulls));
    }
}
