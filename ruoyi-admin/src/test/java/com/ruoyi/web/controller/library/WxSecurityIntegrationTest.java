package com.ruoyi.web.controller.library;

import javax.servlet.FilterChain;
import com.ruoyi.framework.config.SecurityConfig;
import com.ruoyi.framework.config.properties.PermitAllUrlProperties;
import com.ruoyi.framework.security.filter.JwtAuthenticationTokenFilter;
import com.ruoyi.framework.security.handle.AuthenticationEntryPointImpl;
import com.ruoyi.framework.security.handle.LogoutSuccessHandlerImpl;
import com.ruoyi.framework.web.service.TokenService;
import com.ruoyi.library.agreement.WxAgreementInterceptor;
import com.ruoyi.library.agreement.WxAgreementService;
import com.ruoyi.library.auth.WxAuthInterceptor;
import com.ruoyi.library.auth.WxTokenService;
import com.ruoyi.library.auth.WxUserAccessService;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.config.WxWebMvcConfig;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.CorsFilter;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import javax.annotation.Resource;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = WxSecurityIntegrationTest.ProbeController.class)
@Import({SecurityConfig.class, WxWebMvcConfig.class, WxAuthInterceptor.class,
        WxAgreementInterceptor.class, AuthenticationEntryPointImpl.class,
        WxSecurityIntegrationTest.ProbeController.class, WxSecurityIntegrationTest.SecurityTestBeans.class})
class WxSecurityIntegrationTest
{
    @Resource
    private MockMvc mockMvc;

    @MockBean
    private WxTokenService wxTokenService;

    @MockBean
    private WxAgreementService agreementService;

    @MockBean
    private WxUserAccessService userAccessService;

    @MockBean
    private TokenService backendTokenService;

    @MockBean
    private UserDetailsService userDetailsService;

    @BeforeEach
    void setUp()
    {
        when(wxTokenService.resolveWithoutRefresh("valid-token")).thenReturn(7L);
        when(wxTokenService.resolveWithoutRefresh("disabled-token")).thenReturn(8L);
        when(wxTokenService.resolveWithoutRefresh(null)).thenReturn(null);
        when(userAccessService.isEnabled(7L)).thenReturn(true);
        when(userAccessService.isEnabled(8L)).thenReturn(false);
        when(backendTokenService.getLoginUser(any())).thenReturn(null);
    }

    @AfterEach
    void tearDown()
    {
        WxUserContext.clear();
    }

    @Test
    void anonymousLoginAndPublicPathsReachMvc() throws Exception
    {
        mockMvc.perform(post("/wx/auth/login")).andExpect(status().isOk())
                .andExpect(content().string("login-ok"));
        mockMvc.perform(get("/wx/public/ping")).andExpect(status().isOk())
                .andExpect(content().string("public-ok"));
    }

    @Test
    void protectedWechatPathWithoutWxTokenReturnsWechatUnauthorized() throws Exception
    {
        mockMvc.perform(get("/wx/protected"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value(40101));
        verify(wxTokenService).resolveWithoutRefresh(null);
    }

    @Test
    void validTokenWithoutLatestAgreementsReturnsConflict() throws Exception
    {
        when(agreementService.hasAcceptedAllCurrent(7L)).thenReturn(false);
        mockMvc.perform(get("/wx/protected").header("Wx-Token", "valid-token"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(40901));
        assertNull(WxUserContext.get());
    }

    @Test
    void validTokenAndLatestAgreementsReachProtectedController() throws Exception
    {
        when(agreementService.hasAcceptedAllCurrent(7L)).thenReturn(true);
        mockMvc.perform(get("/wx/protected").header("Wx-Token", "valid-token"))
                .andExpect(status().isOk()).andExpect(content().string("wx-ok-7"));
    }

    @Test
    void ordinaryBackendPathStillRequiresRuoYiAuthentication() throws Exception
    {
        mockMvc.perform(get("/admin/protected"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(401));
    }

    @Test
    void disabledWechatUserCannotAccessAgreementAcceptancePath() throws Exception
    {
        mockMvc.perform(post("/wx/agreements/accept").header("Wx-Token", "disabled-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40301));
        verify(wxTokenService, org.mockito.Mockito.never()).refresh("disabled-token");
    }

    @RestController
    static class ProbeController
    {
        @PostMapping("/wx/auth/login")
        String login() { return "login-ok"; }

        @GetMapping("/wx/public/ping")
        String publicPing() { return "public-ok"; }

        @GetMapping("/wx/protected")
        String wxProtected() { return "wx-ok-" + WxUserContext.get(); }

        @GetMapping("/admin/protected")
        String backendProtected() { return "admin-ok"; }

        @PostMapping("/wx/agreements/accept")
        String acceptAgreement() { return "accepted"; }
    }

    @Configuration
    static class SecurityTestBeans
    {
        @Bean
        JwtAuthenticationTokenFilter jwtAuthenticationTokenFilter()
        {
            return new JwtAuthenticationTokenFilter();
        }

        @Bean
        LogoutSuccessHandlerImpl logoutSuccessHandler()
        {
            return new LogoutSuccessHandlerImpl();
        }

        @Bean
        CorsFilter corsFilter()
        {
            return new CorsFilter(new UrlBasedCorsConfigurationSource());
        }

        @Bean
        PermitAllUrlProperties permitAllUrlProperties()
        {
            PermitAllUrlProperties properties = new PermitAllUrlProperties();
            properties.setUrls(java.util.Collections.emptyList());
            return properties;
        }
    }
}
