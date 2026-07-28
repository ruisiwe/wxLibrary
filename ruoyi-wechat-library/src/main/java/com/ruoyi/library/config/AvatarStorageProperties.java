package com.ruoyi.library.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/** 本地头像文件大小和图片尺寸限制。 */
@Component
@ConfigurationProperties(prefix = "wechat.avatar")
public class AvatarStorageProperties
{
    private long maxBytes = 2L * 1024 * 1024;
    private int maxWidth = 2048;
    private int maxHeight = 2048;
    private long maxPixels = 4194304L;

    public long getMaxBytes() { return maxBytes; }
    public void setMaxBytes(long maxBytes) { this.maxBytes = maxBytes; }
    public int getMaxWidth() { return maxWidth; }
    public void setMaxWidth(int maxWidth) { this.maxWidth = maxWidth; }
    public int getMaxHeight() { return maxHeight; }
    public void setMaxHeight(int maxHeight) { this.maxHeight = maxHeight; }
    public long getMaxPixels() { return maxPixels; }
    public void setMaxPixels(long maxPixels) { this.maxPixels = maxPixels; }
}
