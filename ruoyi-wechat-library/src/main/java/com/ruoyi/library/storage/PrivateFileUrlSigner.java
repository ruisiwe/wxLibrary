package com.ruoyi.library.storage;

import java.net.URL;
import java.time.Duration;

/** 私有对象短时下载地址签名端口，由后续 COS 实现提供。 */
public interface PrivateFileUrlSigner
{
    URL signGetUrl(String objectKey, Duration ttl, String downloadFileName);
}
