package com.ruoyi.library.agreement;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONWriter;
import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.auth.WxAuthInterceptor;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** 当前协议确认拦截器。 */
@Component
public class WxAgreementInterceptor implements HandlerInterceptor
{
    private final WxAgreementService agreementService;

    public WxAgreementInterceptor(WxAgreementService agreementService)
    {
        this.agreementService = agreementService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException
    {
        Long userId = WxUserContext.get();
        // 未建立微信用户上下文时由认证拦截器统一返回401，避免协议错误覆盖登录失效提示。
        String token = request.getHeader(WxAuthInterceptor.TOKEN_HEADER);
        if (token == null || token.trim().isEmpty() || userId == null) return true;
        if (agreementService.hasAcceptedAllCurrent(userId)) return true;
        response.setStatus(HttpServletResponse.SC_CONFLICT);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE + ";charset=UTF-8");
        response.getWriter().write(JSON.toJSONString(WxApiResponse.failure(40901,
                "请先阅读并同意最新用户隐私协议"), JSONWriter.Feature.WriteNulls));
        return false;
    }
}
