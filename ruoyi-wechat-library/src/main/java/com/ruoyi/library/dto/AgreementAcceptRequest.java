package com.ruoyi.library.dto;

/** 当前协议确认请求。 */
public class AgreementAcceptRequest
{
    private boolean privacyAccepted;
    private String privacyVersion;
    private boolean statementAccepted;
    private String statementVersion;

    public boolean isPrivacyAccepted() { return privacyAccepted; }
    public void setPrivacyAccepted(boolean privacyAccepted) { this.privacyAccepted = privacyAccepted; }
    public String getPrivacyVersion() { return privacyVersion; }
    public void setPrivacyVersion(String privacyVersion) { this.privacyVersion = privacyVersion; }
    public boolean isStatementAccepted() { return statementAccepted; }
    public void setStatementAccepted(boolean statementAccepted) { this.statementAccepted = statementAccepted; }
    public String getStatementVersion() { return statementVersion; }
    public void setStatementVersion(String statementVersion) { this.statementVersion = statementVersion; }
}
