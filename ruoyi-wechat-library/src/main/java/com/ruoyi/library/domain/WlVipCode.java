package com.ruoyi.library.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/** 会员兑换码摘要记录，不保存明文。 */
public class WlVipCode extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long planId;
    @JsonIgnore
    private String codeDigest;
    private String codeMask;
    private String status;
    private Long usedUserId;
    private Date usedTime;
    private Date expiresTime;
    private String batchNo;
    private Long vipEntitlementId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public String getCodeDigest() { return codeDigest; }
    public void setCodeDigest(String codeDigest) { this.codeDigest = codeDigest; }
    public String getCodeMask() { return codeMask; }
    public void setCodeMask(String codeMask) { this.codeMask = codeMask; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getUsedUserId() { return usedUserId; }
    public void setUsedUserId(Long usedUserId) { this.usedUserId = usedUserId; }
    public Date getUsedTime() { return usedTime; }
    public void setUsedTime(Date usedTime) { this.usedTime = usedTime; }
    public Date getExpiresTime() { return expiresTime; }
    public void setExpiresTime(Date expiresTime) { this.expiresTime = expiresTime; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public Long getVipEntitlementId() { return vipEntitlementId; }
    public void setVipEntitlementId(Long vipEntitlementId) { this.vipEntitlementId = vipEntitlementId; }
}
