package com.ruoyi.library.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/** 会员权益台账。 */
public class WlVipEntitlement extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private String sourceType;
    private String sourceBizNo;
    private Date startTime;
    private Date endTime;
    private Integer grantedDays;
    private Long giftPoints;
    private Long pointRecordId;
    private String status;
    private Date revokedTime;
    private Long operatorId;
    private String reason;
    private Date oldExpireTime;
    private Date newExpireTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getSourceType() { return sourceType; }
    public void setSourceType(String sourceType) { this.sourceType = sourceType; }
    public String getSourceBizNo() { return sourceBizNo; }
    public void setSourceBizNo(String sourceBizNo) { this.sourceBizNo = sourceBizNo; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
    public Integer getGrantedDays() { return grantedDays; }
    public void setGrantedDays(Integer grantedDays) { this.grantedDays = grantedDays; }
    public Long getGiftPoints() { return giftPoints; }
    public void setGiftPoints(Long giftPoints) { this.giftPoints = giftPoints; }
    public Long getPointRecordId() { return pointRecordId; }
    public void setPointRecordId(Long pointRecordId) { this.pointRecordId = pointRecordId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getRevokedTime() { return revokedTime; }
    public void setRevokedTime(Date revokedTime) { this.revokedTime = revokedTime; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public Date getOldExpireTime() { return oldExpireTime; }
    public void setOldExpireTime(Date oldExpireTime) { this.oldExpireTime = oldExpireTime; }
    public Date getNewExpireTime() { return newExpireTime; }
    public void setNewExpireTime(Date newExpireTime) { this.newExpireTime = newExpireTime; }
}
