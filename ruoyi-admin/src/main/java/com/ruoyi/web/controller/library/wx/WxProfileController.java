package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.dto.WxNicknameUpdateRequest;
import com.ruoyi.library.dto.WxProfileResponse;
import com.ruoyi.library.service.WxProfileService;
import org.springframework.http.MediaType;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 小程序当前用户资料接口。 */
@RestController
@RequestMapping("/wx/profile")
public class WxProfileController
{
    private final WxProfileService profileService;

    public WxProfileController(WxProfileService profileService) { this.profileService = profileService; }

    /** 查询当前微信用户资料。 */
    @GetMapping
    public WxApiResponse<WxProfileResponse> get()
    {
        return WxApiResponse.success(profileService.get(WxUserContext.get()));
    }

    /** 使用 JSON 修改当前用户昵称，不接受 openid。 */
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public WxApiResponse<WxProfileResponse> updateNickname(@RequestBody WxNicknameUpdateRequest request)
    {
        return WxApiResponse.success(profileService.update(WxUserContext.get(), request.getNickname(), null));
    }

    /** 使用微信上传接口修改当前用户头像，可同时提交昵称。 */
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public WxApiResponse<WxProfileResponse> updateAvatar(
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestPart("avatar") MultipartFile avatar)
    {
        return WxApiResponse.success(profileService.update(WxUserContext.get(), nickname, avatar));
    }

    /** 拒绝昵称修改接口不支持的请求类型。 */
    @PutMapping
    public WxApiResponse<WxProfileResponse> rejectUnsupportedNicknameMediaType()
            throws HttpMediaTypeNotSupportedException
    {
        throw new HttpMediaTypeNotSupportedException("请求类型不支持");
    }

    /** 拒绝头像修改接口不支持的请求类型。 */
    @PostMapping("/avatar")
    public WxApiResponse<WxProfileResponse> rejectUnsupportedAvatarMediaType()
            throws HttpMediaTypeNotSupportedException
    {
        throw new HttpMediaTypeNotSupportedException("请求类型不支持");
    }
}
