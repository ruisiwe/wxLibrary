package com.ruoyi.library.dto;

import java.util.List;

/** 后台会员开通、续期或补偿请求。 */
public class VipOperationRequest
{
    private List<Long> userIds;
    private Long planId;
    private Integer days;
    private String batchNo;
    private String reason;

    public List<Long> getUserIds() { return userIds; }
    public void setUserIds(List<Long> userIds) { this.userIds = userIds; }
    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Integer getDays() { return days; }
    public void setDays(Integer days) { this.days = days; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
