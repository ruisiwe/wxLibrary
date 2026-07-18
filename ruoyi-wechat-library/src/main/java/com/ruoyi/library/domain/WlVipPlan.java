package com.ruoyi.library.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** 会员套餐。 */
public class WlVipPlan extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String planCode;
    private String planName;
    private Long priceCent;
    private Integer validDays;
    private Long giftPoints;
    private Integer sortOrder;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPlanCode() { return planCode; }
    public void setPlanCode(String planCode) { this.planCode = planCode; }
    public String getPlanName() { return planName; }
    public void setPlanName(String planName) { this.planName = planName; }
    public Long getPriceCent() { return priceCent; }
    public void setPriceCent(Long priceCent) { this.priceCent = priceCent; }
    public Integer getValidDays() { return validDays; }
    public void setValidDays(Integer validDays) { this.validDays = validDays; }
    public Long getGiftPoints() { return giftPoints; }
    public void setGiftPoints(Long giftPoints) { this.giftPoints = giftPoints; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
