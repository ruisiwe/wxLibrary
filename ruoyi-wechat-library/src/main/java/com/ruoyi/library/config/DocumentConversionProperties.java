package com.ruoyi.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 文档转换进程限制配置。 */
@Component
@ConfigurationProperties(prefix = "library.document-conversion")
public class DocumentConversionProperties
{
    private String executable;
    private String tempDirectory;
    private long timeoutSeconds = 120L;
    private long maxInputBytes = 100L * 1024L * 1024L;
    private long maxOutputBytes = 100L * 1024L * 1024L;

    public String getExecutable() { return executable; }
    public void setExecutable(String executable) { this.executable = executable; }
    public String getTempDirectory() { return tempDirectory; }
    public void setTempDirectory(String tempDirectory) { this.tempDirectory = tempDirectory; }
    public long getTimeoutSeconds() { return timeoutSeconds; }
    public void setTimeoutSeconds(long timeoutSeconds) { this.timeoutSeconds = timeoutSeconds; }
    public long getMaxInputBytes() { return maxInputBytes; }
    public void setMaxInputBytes(long maxInputBytes) { this.maxInputBytes = maxInputBytes; }
    public long getMaxOutputBytes() { return maxOutputBytes; }
    public void setMaxOutputBytes(long maxOutputBytes) { this.maxOutputBytes = maxOutputBytes; }
}
