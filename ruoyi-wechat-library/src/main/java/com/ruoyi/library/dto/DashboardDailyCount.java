package com.ruoyi.library.dto;

/** 后台首页某个自然日的统计数量。 */
public class DashboardDailyCount
{
    private String date;
    private Long count;

    public DashboardDailyCount() { }

    public DashboardDailyCount(String date, Long count)
    {
        this.date = date;
        this.count = count;
    }

    public String getDate() { return date; }
    public void setDate(String date) { this.date = date; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
