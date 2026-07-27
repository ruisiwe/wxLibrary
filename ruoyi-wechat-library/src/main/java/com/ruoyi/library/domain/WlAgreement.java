package com.ruoyi.library.domain;

import java.util.Date;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;

/** 用户协议版本。 */
public class WlAgreement extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String agreementType;
    private String version;
    private String title;
    private String content;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private Date effectiveTime;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getAgreementType() { return agreementType; }
    public void setAgreementType(String agreementType) { this.agreementType = agreementType; }
    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public Date getEffectiveTime() { return effectiveTime; }
    public void setEffectiveTime(Date effectiveTime) { this.effectiveTime = effectiveTime; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
