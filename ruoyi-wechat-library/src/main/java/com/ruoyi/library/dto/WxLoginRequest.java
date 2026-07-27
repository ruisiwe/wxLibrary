package com.ruoyi.library.dto;

/** 小程序登录表单。 */
public class WxLoginRequest
{
    private String code;
    private String nickname;
    private boolean privacyAccepted;
    private String privacyVersion;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public boolean isPrivacyAccepted() { return privacyAccepted; }
    public void setPrivacyAccepted(boolean privacyAccepted) { this.privacyAccepted = privacyAccepted; }
    public String getPrivacyVersion() { return privacyVersion; }
    public void setPrivacyVersion(String privacyVersion) { this.privacyVersion = privacyVersion; }

    public boolean hasAgreementSubmission()
    {
        return privacyAccepted || notBlank(privacyVersion);
    }

    private boolean notBlank(String value)
    {
        return value != null && !value.trim().isEmpty();
    }
}
