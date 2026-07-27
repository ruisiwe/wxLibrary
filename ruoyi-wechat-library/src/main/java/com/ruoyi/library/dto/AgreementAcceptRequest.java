package com.ruoyi.library.dto;

/** 当前协议确认请求。 */
public class AgreementAcceptRequest
{
    private boolean privacyAccepted;
    private String privacyVersion;

    public boolean isPrivacyAccepted() { return privacyAccepted; }
    public void setPrivacyAccepted(boolean privacyAccepted) { this.privacyAccepted = privacyAccepted; }
    public String getPrivacyVersion() { return privacyVersion; }
    public void setPrivacyVersion(String privacyVersion) { this.privacyVersion = privacyVersion; }
}
