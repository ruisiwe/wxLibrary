package com.ruoyi.library.dto;

/** 原文件发送前的免责声明确认参数。 */
public class OriginalFileRequest
{
    private Long agreementId;
    private String agreementVersion;
    private boolean confirmed;
    private boolean reminderSuppressed;

    public Long getAgreementId() { return agreementId; }
    public void setAgreementId(Long agreementId) { this.agreementId = agreementId; }
    public String getAgreementVersion() { return agreementVersion; }
    public void setAgreementVersion(String agreementVersion) { this.agreementVersion = agreementVersion; }
    public boolean isConfirmed() { return confirmed; }
    public void setConfirmed(boolean confirmed) { this.confirmed = confirmed; }
    public boolean isReminderSuppressed() { return reminderSuppressed; }
    public void setReminderSuppressed(boolean reminderSuppressed) { this.reminderSuppressed = reminderSuppressed; }
}
