package com.ruoyi.library.dto;

import java.util.Date;

/** 后台会员码生成参数。 */
public class VipCodeGenerateRequest
{
    private Long planId;
    private Integer count;
    private Date expiresTime;

    public Long getPlanId() { return planId; }
    public void setPlanId(Long planId) { this.planId = planId; }
    public Integer getCount() { return count; }
    public void setCount(Integer count) { this.count = count; }
    public Date getExpiresTime() { return expiresTime; }
    public void setExpiresTime(Date expiresTime) { this.expiresTime = expiresTime; }
}
