package com.ruoyi.library.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** VIP 权益介绍。 */
public class WlVipBenefit extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String benefitText;
    private Integer sortOrder;
    private String status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getBenefitText() { return benefitText; }
    public void setBenefitText(String benefitText) { this.benefitText = benefitText; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
