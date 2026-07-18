package com.ruoyi.library.dto;

/** 私有文件短时访问授权。 */
public class FileAuthorization
{
    private final String fileName;
    private final String url;
    private final long expiresInSeconds;

    public FileAuthorization(String fileName, String url, long expiresInSeconds)
    {
        this.fileName = fileName;
        this.url = url;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getFileName() { return fileName; }
    public String getUrl() { return url; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
}
