package com.ruoyi.library.payment;
import java.time.Instant;
/** 已由微信支付 SDK 验签并解密的支付交易。 */
public class PaymentTransaction
{
    private String appId,mchId,outTradeNo,transactionId,currency; private Long amountCent; private Instant successTime;
    public String getAppId(){return appId;} public void setAppId(String v){appId=v;}
    public String getMchId(){return mchId;} public void setMchId(String v){mchId=v;}
    public String getOutTradeNo(){return outTradeNo;} public void setOutTradeNo(String v){outTradeNo=v;}
    public String getTransactionId(){return transactionId;} public void setTransactionId(String v){transactionId=v;}
    public String getCurrency(){return currency;} public void setCurrency(String v){currency=v;}
    public Long getAmountCent(){return amountCent;} public void setAmountCent(Long v){amountCent=v;}
    public Instant getSuccessTime(){return successTime;} public void setSuccessTime(Instant v){successTime=v;}
}
