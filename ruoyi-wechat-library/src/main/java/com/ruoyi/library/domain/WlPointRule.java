package com.ruoyi.library.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 可配置积分规则。 */
public class WlPointRule extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String eventType;
    private String ruleName;
    private Long pointValue;
    private Integer dailyLimit;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getEventType() { return eventType; }
    public void setEventType(String eventType) { this.eventType = eventType; }
    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }
    public Long getPointValue() { return pointValue; }
    public void setPointValue(Long pointValue) { this.pointValue = pointValue; }
    public Integer getDailyLimit() { return dailyLimit; }
    public void setDailyLimit(Integer dailyLimit) { this.dailyLimit = dailyLimit; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
