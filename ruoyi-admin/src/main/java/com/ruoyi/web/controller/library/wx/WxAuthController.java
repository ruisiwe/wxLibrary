package com.ruoyi.web.controller.library.wx;

import javax.servlet.http.HttpServletRequest;
import com.ruoyi.library.auth.WxLoginService;
import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.dto.WxLoginRequest;
import com.ruoyi.library.dto.WxLoginResponse;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 小程序独立登录接口。 */
@RestController
@RequestMapping("/wx/auth")
public class WxAuthController
{
    private final WxLoginService loginService;

    public WxAuthController(WxLoginService loginService) { this.loginService = loginService; }

    /** 使用微信 code 登录，首次登录必须同时上传头像并确认协议。 */
    @PostMapping(value = "/login", consumes = "multipart/form-data")
    public WxApiResponse<WxLoginResponse> login(@RequestPart("request") WxLoginRequest request,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            HttpServletRequest servletRequest)
    {
        return WxApiResponse.success(loginService.login(request, avatar, servletRequest.getRemoteAddr()));
    }
}
