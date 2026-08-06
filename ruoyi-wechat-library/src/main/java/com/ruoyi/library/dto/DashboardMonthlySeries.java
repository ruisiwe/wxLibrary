package com.ruoyi.library.dto;

import java.util.List;

/** 后台首页单个文档分类的连续月份兑换序列。 */
public class DashboardMonthlySeries
{
    private Long categoryId;
    private String categoryName;
    private List<Long> values;

    public DashboardMonthlySeries() { }

    public DashboardMonthlySeries(Long categoryId, String categoryName, List<Long> values)
    {
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.values = values;
    }

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public List<Long> getValues() { return values; }
    public void setValues(List<Long> values) { this.values = values; }
}
