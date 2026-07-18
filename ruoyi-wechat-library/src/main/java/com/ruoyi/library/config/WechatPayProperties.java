package com.ruoyi.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 微信支付外部配置，不在代码中保存任何商户密钥。 */
@Component
@ConfigurationProperties(prefix = "wechat-library.pay")
public class WechatPayProperties
{
    private String appId;
    private String mchId;
    private String apiV3Key;
    private String merchantSerialNumber;
    private String privateKeyPath;
    private String notifyUrl;
    private String refundNotifyUrl;
    private String refundConfirmToken;

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getMchId() { return mchId; }
    public void setMchId(String mchId) { this.mchId = mchId; }
    public String getApiV3Key() { return apiV3Key; }
    public void setApiV3Key(String apiV3Key) { this.apiV3Key = apiV3Key; }
    public String getMerchantSerialNumber() { return merchantSerialNumber; }
    public void setMerchantSerialNumber(String merchantSerialNumber) { this.merchantSerialNumber = merchantSerialNumber; }
    public String getPrivateKeyPath() { return privateKeyPath; }
    public void setPrivateKeyPath(String privateKeyPath) { this.privateKeyPath = privateKeyPath; }
    public String getNotifyUrl() { return notifyUrl; }
    public void setNotifyUrl(String notifyUrl) { this.notifyUrl = notifyUrl; }
    public String getRefundNotifyUrl() { return refundNotifyUrl; }
    public void setRefundNotifyUrl(String refundNotifyUrl) { this.refundNotifyUrl = refundNotifyUrl; }
    public String getRefundConfirmToken() { return refundConfirmToken; }
    public void setRefundConfirmToken(String refundConfirmToken) { this.refundConfirmToken = refundConfirmToken; }
}
