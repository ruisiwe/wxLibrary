package com.ruoyi.library.dto;

import java.util.ArrayList;
import java.util.List;

/** 后台首页最近七日兑换与活跃用户趋势。 */
public class DashboardTrendData
{
    private List<String> dates;
    private List<Long> paidExchangeCounts;
    private List<Long> activeUserCounts;

    public DashboardTrendData() { }

    public DashboardTrendData(List<String> dates, List<Long> paidExchangeCounts,
            List<Long> activeUserCounts)
    {
        this.dates = dates;
        this.paidExchangeCounts = paidExchangeCounts;
        this.activeUserCounts = activeUserCounts;
    }

    public static DashboardTrendData empty()
    {
        return new DashboardTrendData(new ArrayList<String>(),
                new ArrayList<Long>(), new ArrayList<Long>());
    }

    public List<String> getDates() { return dates; }
    public void setDates(List<String> dates) { this.dates = dates; }
    public List<Long> getPaidExchangeCounts() { return paidExchangeCounts; }
    public void setPaidExchangeCounts(List<Long> paidExchangeCounts) { this.paidExchangeCounts = paidExchangeCounts; }
    public List<Long> getActiveUserCounts() { return activeUserCounts; }
    public void setActiveUserCounts(List<Long> activeUserCounts) { this.activeUserCounts = activeUserCounts; }
}
