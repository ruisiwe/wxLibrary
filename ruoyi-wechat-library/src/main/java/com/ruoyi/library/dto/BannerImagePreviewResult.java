package com.ruoyi.library.dto;

/** 管理端轮播图短时预览地址。 */
public class BannerImagePreviewResult
{
    private final String imageUrl;

    public BannerImagePreviewResult(String imageUrl)
    {
        this.imageUrl = imageUrl;
    }

    public String getImageUrl()
    {
        return imageUrl;
    }
}
