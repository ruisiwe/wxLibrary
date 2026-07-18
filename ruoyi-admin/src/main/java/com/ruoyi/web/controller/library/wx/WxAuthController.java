package com.ruoyi.web.controller.library.wx;

import javax.servlet.http.HttpServletRequest;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.auth.WxLoginService;
import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.dto.WxLoginRequest;
import com.ruoyi.library.dto.WxLoginResponse;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

    /** 使用微信上传接口登录，首次登录通过独立表单字段提交资料、协议和头像。 */
    @PostMapping(value = "/login", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public WxApiResponse<WxLoginResponse> loginByMultipart(
            @RequestPart(value = "request", required = false) WxLoginRequest requestPart,
            @RequestParam(value = "code", required = false) String code,
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestParam(value = "privacyAccepted", required = false) Boolean privacyAccepted,
            @RequestParam(value = "privacyVersion", required = false) String privacyVersion,
            @RequestParam(value = "statementAccepted", required = false) Boolean statementAccepted,
            @RequestParam(value = "statementVersion", required = false) String statementVersion,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar,
            HttpServletRequest servletRequest)
    {
        WxLoginRequest request = requestPart == null ? new WxLoginRequest() : requestPart;
        if (code != null) request.setCode(code);
        if (nickname != null) request.setNickname(nickname);
        if (privacyAccepted != null) request.setPrivacyAccepted(privacyAccepted);
        if (privacyVersion != null) request.setPrivacyVersion(privacyVersion);
        if (statementAccepted != null) request.setStatementAccepted(statementAccepted);
        if (statementVersion != null) request.setStatementVersion(statementVersion);
        if (request.getCode() == null || request.getCode().trim().isEmpty())
            throw new ServiceException("请求参数不完整");
        return WxApiResponse.success(loginService.login(request, avatar, servletRequest.getRemoteAddr()));
    }

    /** 使用 JSON 登录，适用于已完成资料登记的微信用户。 */
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public WxApiResponse<WxLoginResponse> loginByJson(@RequestBody WxLoginRequest request,
            HttpServletRequest servletRequest)
    {
        return WxApiResponse.success(loginService.login(request, null, servletRequest.getRemoteAddr()));
    }

    /** 拒绝登录接口不支持的请求类型。 */
    @PostMapping("/login")
    public WxApiResponse<WxLoginResponse> rejectUnsupportedMediaType()
            throws HttpMediaTypeNotSupportedException
    {
        throw new HttpMediaTypeNotSupportedException("请求类型不支持");
    }
}
