package com.ruoyi.library.dto;

import com.ruoyi.library.domain.WlVipOrder;
import java.util.Date;

/** 当前微信用户可见的会员订单状态。 */
public class VipOrderStatusView
{
    private final String merchantOrderNo;
    private final String orderStatus;
    private final Date paidTime;

    public VipOrderStatusView(WlVipOrder order)
    {
        merchantOrderNo = order.getMerchantOrderNo();
        orderStatus = order.getOrderStatus();
        paidTime = order.getPaidTime();
    }

    public String getMerchantOrderNo() { return merchantOrderNo; }
    public String getOrderStatus() { return orderStatus; }
    public Date getPaidTime() { return paidTime; }
}
