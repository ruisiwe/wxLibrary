package com.ruoyi.library.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/** VIP 套餐页面客服配置。 */
public class WlVipPageConfig extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String customerServiceImageKey;
    private String customerServiceTip;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCustomerServiceImageKey() { return customerServiceImageKey; }
    public void setCustomerServiceImageKey(String customerServiceImageKey)
    {
        this.customerServiceImageKey = customerServiceImageKey;
    }
    public String getCustomerServiceTip() { return customerServiceTip; }
    public void setCustomerServiceTip(String customerServiceTip) { this.customerServiceTip = customerServiceTip; }
}
