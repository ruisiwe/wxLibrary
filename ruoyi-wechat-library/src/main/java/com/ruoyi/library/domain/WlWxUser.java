package com.ruoyi.library.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;

/** 微信用户，仅映射 wl_wx_user，不关联 sys_user。 */
public class WlWxUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    @JsonIgnore
    private String openid;
    @JsonIgnore
    private String unionid;
    private String nickname;
    private String avatarPath;
    private Long pointBalance;
    private Date vipExpireTime;
    private String status;
    private Date lastLoginTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getUnionid() { return unionid; }
    public void setUnionid(String unionid) { this.unionid = unionid; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatarPath() { return avatarPath; }
    public void setAvatarPath(String avatarPath) { this.avatarPath = avatarPath; }
    public Long getPointBalance() { return pointBalance; }
    public void setPointBalance(Long pointBalance) { this.pointBalance = pointBalance; }
    public Date getVipExpireTime() { return vipExpireTime; }
    public void setVipExpireTime(Date vipExpireTime) { this.vipExpireTime = vipExpireTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(Date lastLoginTime) { this.lastLoginTime = lastLoginTime; }
}
