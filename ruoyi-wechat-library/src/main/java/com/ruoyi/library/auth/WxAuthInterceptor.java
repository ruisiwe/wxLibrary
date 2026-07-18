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
    private static final int DISABLED_USER_CODE = 40301;
    private static final String DISABLED_USER_MESSAGE = "该微信用户已被停用，无法访问";

    private final WxTokenService tokenService;
    private final WxUserAccessService userAccessService;

    public WxAuthInterceptor(WxTokenService tokenService, WxUserAccessService userAccessService)
    {
        this.tokenService = tokenService;
        this.userAccessService = userAccessService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException
    {
        WxUserContext.clear();
        Long userId;
        try
        {
            userId = tokenService.resolveWithoutRefresh(request.getHeader(TOKEN_HEADER));
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
        String token = request.getHeader(TOKEN_HEADER);
        if (!userAccessService.isEnabled(userId))
        {
            tokenService.revoke(token);
            writeError(response, HttpServletResponse.SC_FORBIDDEN, DISABLED_USER_CODE, DISABLED_USER_MESSAGE);
            return false;
        }
        tokenService.refresh(token);
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
        writeError(response, HttpServletResponse.SC_UNAUTHORIZED, INVALID_TOKEN_CODE, INVALID_TOKEN_MESSAGE);
    }

    private void writeError(HttpServletResponse response, int status, int code, String message) throws IOException
    {
        response.setStatus(status);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(
                WxApiResponse.failure(code, message), JSONWriter.Feature.WriteNulls));
    }
}
