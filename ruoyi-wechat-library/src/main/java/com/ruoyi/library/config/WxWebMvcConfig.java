package com.ruoyi.library.config;

import com.ruoyi.library.auth.WxAuthInterceptor;
import com.ruoyi.library.agreement.WxAgreementInterceptor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 小程序接口拦截配置，不改变若依管理端鉴权链路。
 */
@Configuration
public class WxWebMvcConfig implements WebMvcConfigurer
{
    private final WxAuthInterceptor wxAuthInterceptor;
    private final WxAgreementInterceptor wxAgreementInterceptor;

    public WxWebMvcConfig(WxAuthInterceptor wxAuthInterceptor, WxAgreementInterceptor wxAgreementInterceptor)
    {
        this.wxAuthInterceptor = wxAuthInterceptor;
        this.wxAgreementInterceptor = wxAgreementInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(wxAuthInterceptor)
                .order(0)
                .addPathPatterns("/wx/**")
                .excludePathPatterns("/wx/auth/login", "/wx/public/**", "/wx/pay/notify", "/wx/pay/notify/**");
        registry.addInterceptor(wxAgreementInterceptor)
                .order(1)
                .addPathPatterns("/wx/**")
                .excludePathPatterns("/wx/auth/login", "/wx/public/**", "/wx/pay/notify", "/wx/pay/notify/**",
                        "/wx/agreements/accept");
    }
}
