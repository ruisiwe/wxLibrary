package com.ruoyi.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信登录配置。敏感值仅从外部配置注入，禁止写入代码或日志。
 */
@Component
@ConfigurationProperties(prefix = "wechat.login")
public class WechatLoginProperties
{
    private String appId;
    private String secret;
    private String code2SessionUrl = "https://api.weixin.qq.com/sns/jscode2session";
    private int connectTimeoutMillis = 3000;
    private int readTimeoutMillis = 5000;

    public String getAppId() { return appId; }
    public void setAppId(String appId) { this.appId = appId; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public String getCode2SessionUrl() { return code2SessionUrl; }
    public void setCode2SessionUrl(String code2SessionUrl) { this.code2SessionUrl = code2SessionUrl; }
    public int getConnectTimeoutMillis() { return connectTimeoutMillis; }
    public void setConnectTimeoutMillis(int connectTimeoutMillis) { this.connectTimeoutMillis = connectTimeoutMillis; }
    public int getReadTimeoutMillis() { return readTimeoutMillis; }
    public void setReadTimeoutMillis(int readTimeoutMillis) { this.readTimeoutMillis = readTimeoutMillis; }
}
