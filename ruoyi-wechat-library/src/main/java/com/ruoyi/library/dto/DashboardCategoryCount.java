package com.ruoyi.library.dto;

/** 后台首页按文档分类汇总的数量。 */
public class DashboardCategoryCount
{
    private Long categoryId;
    private String categoryName;
    private Long count;

    public DashboardCategoryCount() { }

    public DashboardCategoryCount(Long categoryId, String categoryName, Long count)
    {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.count = count;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
