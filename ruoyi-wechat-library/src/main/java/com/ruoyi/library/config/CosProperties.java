package com.ruoyi.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 腾讯云 COS 私有桶配置，只接受外部运行环境注入。 */
@Component
@ConfigurationProperties(prefix = "library.cos")
public class CosProperties
{
    private String region;
    private String bucket;
    private String secretId;
    private String secretKey;

    public String getRegion() { return region; }
    public void setRegion(String region) { this.region = region; }
    public String getBucket() { return bucket; }
    public void setBucket(String bucket) { this.bucket = bucket; }
    public String getSecretId() { return secretId; }
    public void setSecretId(String secretId) { this.secretId = secretId; }
    public String getSecretKey() { return secretKey; }
    public void setSecretKey(String secretKey) { this.secretKey = secretKey; }
}
