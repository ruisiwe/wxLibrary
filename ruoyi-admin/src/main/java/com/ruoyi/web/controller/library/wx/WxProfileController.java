package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.dto.WxProfileResponse;
import com.ruoyi.library.service.WxProfileService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
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

    /** 修改昵称或头像，不接受 openid。 */
    @PutMapping(consumes = "multipart/form-data")
    public WxApiResponse<WxProfileResponse> update(
            @RequestParam(value = "nickname", required = false) String nickname,
            @RequestPart(value = "avatar", required = false) MultipartFile avatar)
    {
        return WxApiResponse.success(profileService.update(WxUserContext.get(), nickname, avatar));
    }
}
