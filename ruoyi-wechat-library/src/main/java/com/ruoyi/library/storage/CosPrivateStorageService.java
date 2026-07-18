package com.ruoyi.library.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.ClientConfig;
import com.qcloud.cos.auth.BasicCOSCredentials;
import com.qcloud.cos.auth.COSCredentials;
import com.qcloud.cos.http.HttpMethodName;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.qcloud.cos.model.ResponseHeaderOverrides;
import com.qcloud.cos.region.Region;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.CosProperties;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.regex.Pattern;
import javax.annotation.PreDestroy;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 腾讯云 COS 私有对象上传、删除和短时签名服务。 */
@Service
public class CosPrivateStorageService implements PrivateFileUrlSigner
{
    private static final Pattern SAFE_KEY = Pattern.compile("[A-Za-z0-9][A-Za-z0-9._/-]{0,511}");
    private final CosProperties properties;
    private volatile COSClient client;

    @Autowired
    public CosPrivateStorageService(CosProperties properties) { this.properties = properties; }

    CosPrivateStorageService(CosProperties properties, COSClient client)
    {
        this.properties = properties;
        this.client = client;
    }

    /** 上传私有对象，只返回对象键。 */
    public String putPrivateObject(String objectKey, InputStream input, long size, String contentType)
    {
        validateObjectKey(objectKey);
        if (input == null) throw new ServiceException("上传文件内容不能为空");
        if (size < 1) throw new ServiceException("上传文件不能为空");
        requireText(contentType, "上传文件类型不能为空");
        ObjectMetadata metadata = new ObjectMetadata();
        metadata.setContentLength(size);
        metadata.setContentType(contentType.trim());
        getClient().putObject(new PutObjectRequest(bucket(), objectKey, input, metadata));
        return objectKey;
    }

    /** 签发短时 GET 地址，可附带安全下载文件名。 */
    @Override
    public URL signGetUrl(String objectKey, Duration ttl, String downloadFileName)
    {
        validateObjectKey(objectKey);
        if (ttl == null || ttl.isZero() || ttl.isNegative() || ttl.compareTo(Duration.ofHours(1)) > 0)
            throw new ServiceException("文件地址有效期不正确");
        GeneratePresignedUrlRequest request = new GeneratePresignedUrlRequest(
                bucket(), objectKey, HttpMethodName.GET);
        request.setExpiration(new Date(System.currentTimeMillis() + ttl.toMillis()));
        if (downloadFileName != null && !downloadFileName.trim().isEmpty())
        {
            String encoded = encodeFileName(downloadFileName);
            ResponseHeaderOverrides headers = new ResponseHeaderOverrides();
            headers.setContentDisposition("attachment; filename*=UTF-8''" + encoded);
            request.setResponseHeaders(headers);
        }
        return getClient().generatePresignedUrl(request);
    }

    /** 元数据删除成功后删除关联私有对象。 */
    public void deleteObjectAfterMetadataDeletion(String objectKey)
    {
        if (objectKey == null || objectKey.trim().isEmpty()) return;
        validateObjectKey(objectKey);
        getClient().deleteObject(bucket(), objectKey);
    }

    @PreDestroy
    public void close()
    {
        COSClient current = client;
        if (current != null) current.shutdown();
    }

    private COSClient getClient()
    {
        COSClient current = client;
        if (current != null) return current;
        synchronized (this)
        {
            if (client == null)
            {
                requireText(properties.getRegion(), "COS 地域未配置");
                requireText(properties.getSecretId(), "COS 访问标识未配置");
                requireText(properties.getSecretKey(), "COS 访问密钥未配置");
                COSCredentials credentials = new BasicCOSCredentials(
                        properties.getSecretId().trim(), properties.getSecretKey().trim());
                client = new COSClient(credentials,
                        new ClientConfig(new Region(properties.getRegion().trim())));
            }
            return client;
        }
    }

    private String bucket()
    {
        requireText(properties.getBucket(), "COS 私有桶未配置");
        return properties.getBucket().trim();
    }

    private void validateObjectKey(String objectKey)
    {
        if (objectKey == null || !SAFE_KEY.matcher(objectKey).matches()
                || objectKey.startsWith("/") || objectKey.contains("//")
                || objectKey.contains("../") || objectKey.endsWith("/.."))
            throw new ServiceException("COS 对象键不正确");
    }

    private String safeFileName(String value)
    {
        String fileName = value.replaceAll("[\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (fileName.isEmpty()) fileName = "文件";
        return fileName.length() <= 180 ? fileName : fileName.substring(0, 180);
    }

    private String encodeFileName(String value)
    {
        try
        {
            return URLEncoder.encode(safeFileName(value), StandardCharsets.UTF_8.name()).replace("+", "%20");
        }
        catch (UnsupportedEncodingException exception)
        {
            throw new ServiceException("下载文件名编码失败");
        }
    }

    private void requireText(String value, String message)
    {
        if (value == null || value.trim().isEmpty()) throw new ServiceException(message);
    }
}
