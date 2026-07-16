package com.ruoyi.library.dto;

import com.ruoyi.library.domain.WlWxUser;

/** 小程序用户资料，不包含微信 openid。 */
public class WxProfileResponse
{
    private final Long userId;
    private final String nickname;
    private final String avatarPath;
    private final Long pointBalance;

    public WxProfileResponse(WlWxUser user)
    {
        this.userId = user.getId();
        this.nickname = user.getNickname();
        this.avatarPath = user.getAvatarPath();
        this.pointBalance = user.getPointBalance();
    }

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getAvatarPath() { return avatarPath; }
    public Long getPointBalance() { return pointBalance; }
}
