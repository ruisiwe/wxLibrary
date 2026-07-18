package com.ruoyi.library.dto;

import java.util.Date;
import com.ruoyi.library.domain.WlWxUser;

/** 后台微信用户视图，仅展示脱敏 openid。 */
public class LibraryWxUserView
{
    private Long id;
    private String openidMasked;
    private String nickname;
    private String avatarPath;
    private Long pointBalance;
    private Date vipExpireTime;
    private String status;
    private Date lastLoginTime;

    public static LibraryWxUserView from(WlWxUser user)
    {
        if (user == null) return null;
        LibraryWxUserView view = new LibraryWxUserView();
        view.id = user.getId();
        view.openidMasked = mask(user.getOpenid());
        view.nickname = user.getNickname();
        view.avatarPath = user.getAvatarPath();
        view.pointBalance = user.getPointBalance();
        view.vipExpireTime = user.getVipExpireTime();
        view.status = user.getStatus();
        view.lastLoginTime = user.getLastLoginTime();
        return view;
    }

    private static String mask(String value)
    {
        if (value == null || value.length() < 8) return "****";
        return value.substring(0, 4) + "****" + value.substring(value.length() - 4);
    }

    public Long getId() { return id; }
    public String getOpenidMasked() { return openidMasked; }
    public String getNickname() { return nickname; }
    public String getAvatarPath() { return avatarPath; }
    public Long getPointBalance() { return pointBalance; }
    public Date getVipExpireTime() { return vipExpireTime; }
    public String getStatus() { return status; }
    public Date getLastLoginTime() { return lastLoginTime; }
}
