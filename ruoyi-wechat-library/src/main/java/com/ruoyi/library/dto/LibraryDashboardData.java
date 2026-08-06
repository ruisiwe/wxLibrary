package com.ruoyi.library.dto;

import java.util.List;

/** 后台首页全部文库统计数据。 */
public class LibraryDashboardData
{
    private DashboardSummary summary;
    private DashboardMonthlyData monthlyPaidExchanges;
    private DashboardTrendData sevenDayTrend;
    private List<DashboardCategoryCount> categoryDocumentCounts;
    private List<DashboardCategoryCount> categorySendShares;

    public LibraryDashboardData() { }

    public LibraryDashboardData(DashboardSummary summary, DashboardMonthlyData monthlyPaidExchanges,
            DashboardTrendData sevenDayTrend, List<DashboardCategoryCount> categoryDocumentCounts,
            List<DashboardCategoryCount> categorySendShares)
    {
        this.summary = summary;
        this.monthlyPaidExchanges = monthlyPaidExchanges;
        this.sevenDayTrend = sevenDayTrend;
        this.categoryDocumentCounts = categoryDocumentCounts;
        this.categorySendShares = categorySendShares;
    }

    public DashboardSummary getSummary() { return summary; }
    public void setSummary(DashboardSummary summary) { this.summary = summary; }
    public DashboardMonthlyData getMonthlyPaidExchanges() { return monthlyPaidExchanges; }
    public void setMonthlyPaidExchanges(DashboardMonthlyData monthlyPaidExchanges) { this.monthlyPaidExchanges = monthlyPaidExchanges; }
    public DashboardTrendData getSevenDayTrend() { return sevenDayTrend; }
    public void setSevenDayTrend(DashboardTrendData sevenDayTrend) { this.sevenDayTrend = sevenDayTrend; }
    public List<DashboardCategoryCount> getCategoryDocumentCounts() { return categoryDocumentCounts; }
    public void setCategoryDocumentCounts(List<DashboardCategoryCount> categoryDocumentCounts) { this.categoryDocumentCounts = categoryDocumentCounts; }
    public List<DashboardCategoryCount> getCategorySendShares() { return categorySendShares; }
    public void setCategorySendShares(List<DashboardCategoryCount> categorySendShares) { this.categorySendShares = categorySendShares; }
}
