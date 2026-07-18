package com.ruoyi.library.payment;
import java.time.Instant;
/** 已由微信支付 SDK 验签并解密的退款通知。 */
public class RefundNotification
{
    private String merchantRefundNo,wechatRefundId,outTradeNo,currency,status; private Long refundAmountCent; private Instant successTime;
    public String getMerchantRefundNo(){return merchantRefundNo;} public void setMerchantRefundNo(String v){merchantRefundNo=v;}
    public String getWechatRefundId(){return wechatRefundId;} public void setWechatRefundId(String v){wechatRefundId=v;}
    public String getOutTradeNo(){return outTradeNo;} public void setOutTradeNo(String v){outTradeNo=v;}
    public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
    public String getStatus(){return status;} public void setStatus(String v){status=v;}
    public Long getRefundAmountCent(){return refundAmountCent;} public void setRefundAmountCent(Long v){refundAmountCent=v;}
    public Instant getSuccessTime(){return successTime;} public void setSuccessTime(Instant v){successTime=v;}
}
