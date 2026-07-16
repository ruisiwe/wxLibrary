package com.ruoyi.library.config;

import com.ruoyi.library.auth.WxAuthInterceptor;
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

    public WxWebMvcConfig(WxAuthInterceptor wxAuthInterceptor)
    {
        this.wxAuthInterceptor = wxAuthInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry)
    {
        registry.addInterceptor(wxAuthInterceptor)
                .addPathPatterns("/wx/**")
                .excludePathPatterns("/wx/auth/login", "/wx/public/**", "/wx/pay/notify");
    }
}
