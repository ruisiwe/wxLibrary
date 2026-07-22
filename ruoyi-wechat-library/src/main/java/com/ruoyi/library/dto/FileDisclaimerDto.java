package com.ruoyi.library.dto;

/** 当前文件发送免责声明及免提示状态。 */
public class FileDisclaimerDto
{
    private Long agreementId;
    private String agreementVersion;
    private String title;
    private String content;
    private boolean reminderSuppressed;

    public Long getAgreementId() { return agreementId; }
    public void setAgreementId(Long agreementId) { this.agreementId = agreementId; }
    public String getAgreementVersion() { return agreementVersion; }
    public void setAgreementVersion(String agreementVersion) { this.agreementVersion = agreementVersion; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isReminderSuppressed() { return reminderSuppressed; }
    public void setReminderSuppressed(boolean reminderSuppressed) { this.reminderSuppressed = reminderSuppressed; }
}
