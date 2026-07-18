package com.ruoyi.library.dto;
/** 后台全额退款请求。 */
public class VipRefundRequest
{
    private Long orderId; private String reason; private String confirmationToken;
    public Long getOrderId(){return orderId;} public void setOrderId(Long v){orderId=v;}
    public String getReason(){return reason;} public void setReason(String v){reason=v;}
    public String getConfirmationToken(){return confirmationToken;} public void setConfirmationToken(String v){confirmationToken=v;}
}
