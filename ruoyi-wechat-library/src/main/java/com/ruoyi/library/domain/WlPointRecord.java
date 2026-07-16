package com.ruoyi.library.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 积分变更流水。 */
public class WlPointRecord extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Long ruleId;
    private String eventType;
    private String bizNo;
    private Long changePoints;
    private Long beforeBalance;
    private Long afterBalance;
    private String description;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getRuleId() { return ruleId; }
    public void setRuleId(Long ruleId) { this.ruleId = ruleId; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public Long getChangePoints() { return changePoints; }
    public void setChangePoints(Long changePoints) { this.changePoints = changePoints; }
    public Long getBeforeBalance() { return beforeBalance; }
    public void setBeforeBalance(Long beforeBalance) { this.beforeBalance = beforeBalance; }
    public Long getAfterBalance() { return afterBalance; }
    public void setAfterBalance(Long afterBalance) { this.afterBalance = afterBalance; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
