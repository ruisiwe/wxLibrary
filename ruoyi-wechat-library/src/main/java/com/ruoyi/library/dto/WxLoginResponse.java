package com.ruoyi.library.dto;

/** 小程序登录结果，禁止包含 openid。 */
public class WxLoginResponse
{
    private final String token;
    private final Long userId;
    private final String nickname;
    private final String avatarPath;
    private final Long pointBalance;
    private final boolean agreementRequired;

    public WxLoginResponse(String token, Long userId, String nickname, String avatarPath,
            Long pointBalance, boolean agreementRequired)
    {
        this.token = token;
        this.userId = userId;
        this.nickname = nickname;
        this.avatarPath = avatarPath;
        this.pointBalance = pointBalance;
        this.agreementRequired = agreementRequired;
    }

    public String getToken() { return token; }
    public Long getUserId() { return userId; }
    public String getNickname() { return nickname; }
    public String getAvatarPath() { return avatarPath; }
    public Long getPointBalance() { return pointBalance; }
    public boolean isAgreementRequired() { return agreementRequired; }
}
