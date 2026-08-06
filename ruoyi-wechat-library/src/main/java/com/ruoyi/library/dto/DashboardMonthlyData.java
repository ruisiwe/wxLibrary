package com.ruoyi.library.dto;

import java.util.ArrayList;
import java.util.List;

/** 后台首页连续月份及分类堆叠序列。 */
public class DashboardMonthlyData
{
    private List<String> months;
    private List<DashboardMonthlySeries> series;

    public DashboardMonthlyData() { }

    public DashboardMonthlyData(List<String> months, List<DashboardMonthlySeries> series)
    {
        this.months = months;
        this.series = series;
    }

    public static DashboardMonthlyData empty()
    {
        return new DashboardMonthlyData(new ArrayList<String>(),
                new ArrayList<DashboardMonthlySeries>());
    }

    public List<String> getMonths() { return months; }
    public void setMonths(List<String> months) { this.months = months; }
    public List<DashboardMonthlySeries> getSeries() { return series; }
    public void setSeries(List<DashboardMonthlySeries> series) { this.series = series; }
}
