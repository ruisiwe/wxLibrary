package com.ruoyi.library.dto;

import com.ruoyi.library.domain.WlWxUser;
import java.util.Date;

/** VIP 操作用户候选项。 */
public class VipUserOptionView
{
    private Long id;
    private String nickname;
    private String avatarPath;
    private Date vipExpireTime;

    public static VipUserOptionView from(WlWxUser user)
    {
        VipUserOptionView view = new VipUserOptionView();
        view.setId(user.getId());
        view.setNickname(user.getNickname());
        view.setAvatarPath(user.getAvatarPath());
        view.setVipExpireTime(user.getVipExpireTime());
        return view;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public Date getVipExpireTime() { return vipExpireTime; }
    public void setVipExpireTime(Date vipExpireTime) { this.vipExpireTime = vipExpireTime; }
}
