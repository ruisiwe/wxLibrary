package com.ruoyi.library.dto;

/** 后台会员开通、续期或补偿请求。 */
public class VipOperationRequest
{
    private Long userId;
    private Long planId;
    private Integer days;
    private String bizNo;
    private String reason;

    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public String getBizNo() { return bizNo; }
    public void setBizNo(String bizNo) { this.bizNo = bizNo; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
