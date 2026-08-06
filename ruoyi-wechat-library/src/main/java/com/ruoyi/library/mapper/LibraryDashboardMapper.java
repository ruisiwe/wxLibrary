package com.ruoyi.library.mapper;

import java.util.Date;
import java.util.List;
import com.ruoyi.library.dto.DashboardCategoryCount;
import com.ruoyi.library.dto.DashboardDailyCount;
import com.ruoyi.library.dto.DashboardPeriodCategoryCount;
import com.ruoyi.library.dto.DashboardSummary;
import org.apache.ibatis.annotations.Param;

/** 后台首页文库统计数据访问。 */
public interface LibraryDashboardMapper
{
    DashboardSummary selectSummary(@Param("now") Date now);
    List<DashboardPeriodCategoryCount> selectMonthlyPaidExchangeCounts(
            @Param("start") Date start, @Param("end") Date end);
    List<DashboardDailyCount> selectDailyPaidExchangeCounts(
            @Param("start") Date start, @Param("end") Date end);
    List<DashboardDailyCount> selectDailyActiveUserCounts(
            @Param("startDate") String startDate, @Param("endDate") String endDate);
    List<DashboardCategoryCount> selectCategoryDocumentCounts();
    List<DashboardCategoryCount> selectCategorySendCounts();
}
