package com.ruoyi.library.dto;

import com.ruoyi.library.domain.WlWxUser;
import java.util.Date;

/** 小程序用户资料，不包含微信 openid。 */
public class WxProfileResponse
{
    private final Long userId;
    private final String nickname;
    private final String avatarPath;
    private final Long pointBalance;
    private final Date vipExpireTime;
    private final boolean vipActive;

    public WxProfileResponse(WlWxUser user)
    {
        this.userId = user.getId();
        this.nickname = user.getNickname();
        this.avatarPath = user.getAvatarPath();
        this.pointBalance = user.getPointBalance();
        this.vipExpireTime = user.getVipExpireTime();
        this.vipActive = vipExpireTime != null && vipExpireTime.after(new Date());
    }

    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getAvatarPath() { return avatarPath; }
    public Long getPointBalance() { return pointBalance; }
    public Date getVipExpireTime() { return vipExpireTime; }
    public boolean isVipActive() { return vipActive; }
}
