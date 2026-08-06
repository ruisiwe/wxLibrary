package com.ruoyi.library.dto;

/** 后台首页某月份、某文档分类的兑换次数。 */
public class DashboardPeriodCategoryCount
{
    private String period;
    private Long categoryId;
    private String categoryName;
    private Long count;

    public DashboardPeriodCategoryCount() { }

    public DashboardPeriodCategoryCount(String period, Long categoryId, String categoryName, Long count)
    {
        this.period = period;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.count = count;
    }

    public String getPeriod() { return period; }
    public void setPeriod(String period) { this.period = period; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
