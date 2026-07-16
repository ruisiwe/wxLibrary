package com.ruoyi.library.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/** 微信用户协议确认记录。 */
public class WlUserAgreement extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Long agreementId;
    private String agreementType;
    private String agreementVersion;
    private Date acceptedTime;
    private String acceptedIp;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getAgreementId() { return agreementId; }
    public void setAgreementId(Long agreementId) { this.agreementId = agreementId; }
    public String getAgreementType() { return agreementType; }
    public void setAgreementType(String agreementType) { this.agreementType = agreementType; }
    public String getAgreementVersion() { return agreementVersion; }
    public void setAgreementVersion(String agreementVersion) { this.agreementVersion = agreementVersion; }
    public Date getAcceptedTime() { return acceptedTime; }
    public void setAcceptedTime(Date acceptedTime) { this.acceptedTime = acceptedTime; }
    public String getAcceptedIp() { return acceptedIp; }
    public void setAcceptedIp(String acceptedIp) { this.acceptedIp = acceptedIp; }
}
