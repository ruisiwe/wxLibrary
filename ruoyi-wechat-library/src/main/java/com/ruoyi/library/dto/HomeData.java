package com.ruoyi.library.dto;

import java.util.List;

/** 小程序匿名首页数据。 */
public class HomeData
{
    private final List<BannerDto> banners;
    private final List<CategoryDto> categories;
    private final List<DocumentSummaryDto> documents;

    public HomeData(List<BannerDto> banners, List<CategoryDto> categories,
            List<DocumentSummaryDto> documents)
    {
        this.banners = banners;
        this.categories = categories;
        this.documents = documents;
    }

    public List<BannerDto> getBanners() { return banners; }
    public List<CategoryDto> getCategories() { return categories; }
    public List<DocumentSummaryDto> getDocuments() { return documents; }
}
