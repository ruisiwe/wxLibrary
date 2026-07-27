package com.ruoyi.library.dto;

import java.util.ArrayList;
import java.util.List;

/** VIP 套餐页面权益与客服展示数据。 */
public class VipPageConfigView
{
    private List<String> benefits = new ArrayList<>();
    private String customerServiceTip;
    private String customerServiceImageUrl;

    public VipPageConfigView() { }

    public VipPageConfigView(List<String> benefits, String customerServiceTip,
            String customerServiceImageUrl)
    {
        this.benefits = benefits == null ? new ArrayList<>() : benefits;
        this.customerServiceTip = customerServiceTip;
        this.customerServiceImageUrl = customerServiceImageUrl;
    }

    public List<String> getBenefits() { return benefits; }
    public void setBenefits(List<String> benefits)
    {
        this.benefits = benefits == null ? new ArrayList<>() : benefits;
    }
    public String getCustomerServiceTip() { return customerServiceTip; }
    public void setCustomerServiceTip(String customerServiceTip) { this.customerServiceTip = customerServiceTip; }
    public String getCustomerServiceImageUrl() { return customerServiceImageUrl; }
    public void setCustomerServiceImageUrl(String customerServiceImageUrl)
    {
        this.customerServiceImageUrl = customerServiceImageUrl;
    }
}
