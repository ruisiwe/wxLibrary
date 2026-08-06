package com.ruoyi.library.service;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import com.ruoyi.library.dto.DashboardCategoryCount;
import com.ruoyi.library.dto.DashboardDailyCount;
import com.ruoyi.library.dto.DashboardPeriodCategoryCount;
import com.ruoyi.library.dto.DashboardSummary;
import com.ruoyi.library.dto.LibraryDashboardData;
import com.ruoyi.library.mapper.LibraryDashboardMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LibraryDashboardServiceTest
{
    private LibraryDashboardMapper mapper;
    private LibraryDashboardService service;

    @BeforeEach
    void setUp()
    {
        mapper = mock(LibraryDashboardMapper.class);
        Clock clock = Clock.fixed(Instant.parse("2026-08-03T04:00:00Z"),
                ZoneId.of("Asia/Shanghai"));
        service = new LibraryDashboardService(mapper, clock);
    }

    @Test
    void loadsSummaryAndFillsMissingMonthAndDayBuckets()
    {
        when(mapper.selectSummary(any())).thenReturn(new DashboardSummary(10L, 3L, 20L, 8L));
        when(mapper.selectCategoryDocumentCounts()).thenReturn(Arrays.asList(
                new DashboardCategoryCount(1L, "行业标准", 6L),
                new DashboardCategoryCount(2L, "管理制度", 4L)));
        when(mapper.selectMonthlyPaidExchangeCounts(any(), any())).thenReturn(Arrays.asList(
                new DashboardPeriodCategoryCount("2025-09", 1L, "行业标准", 2L),
                new DashboardPeriodCategoryCount("2026-08", 2L, "管理制度", 5L)));
        when(mapper.selectDailyPaidExchangeCounts(any(), any())).thenReturn(Arrays.asList(
                new DashboardDailyCount("2026-07-28", 3L),
                new DashboardDailyCount("2026-08-03", 7L)));
        when(mapper.selectDailyActiveUserCounts("2026-07-28", "2026-08-04"))
                .thenReturn(Collections.singletonList(new DashboardDailyCount("2026-07-29", 4L)));
        when(mapper.selectCategorySendCounts()).thenReturn(
                Collections.singletonList(new DashboardCategoryCount(2L, "管理制度", 9L)));

        LibraryDashboardData result = service.load();

        assertEquals(10L, result.getSummary().getUserCount());
        assertEquals(3L, result.getSummary().getMemberCount());
        assertEquals(20L, result.getSummary().getDocumentCount());
        assertEquals(8L, result.getSummary().getPaidDocumentCount());
        assertEquals(12, result.getMonthlyPaidExchanges().getMonths().size());
        assertEquals("2025-09", result.getMonthlyPaidExchanges().getMonths().get(0));
        assertEquals("2026-08", result.getMonthlyPaidExchanges().getMonths().get(11));
        assertEquals(Arrays.asList(2L, 0L), Arrays.asList(
                result.getMonthlyPaidExchanges().getSeries().get(0).getValues().get(0),
                result.getMonthlyPaidExchanges().getSeries().get(0).getValues().get(11)));
        assertEquals(Arrays.asList(0L, 5L), Arrays.asList(
                result.getMonthlyPaidExchanges().getSeries().get(1).getValues().get(0),
                result.getMonthlyPaidExchanges().getSeries().get(1).getValues().get(11)));
        assertEquals(Arrays.asList("2026-07-28", "2026-08-03"), Arrays.asList(
                result.getSevenDayTrend().getDates().get(0),
                result.getSevenDayTrend().getDates().get(6)));
        assertEquals(Arrays.asList(3L, 0L, 0L, 0L, 0L, 0L, 7L),
                result.getSevenDayTrend().getPaidExchangeCounts());
        assertEquals(Arrays.asList(0L, 4L, 0L, 0L, 0L, 0L, 0L),
                result.getSevenDayTrend().getActiveUserCounts());
        assertEquals(2, result.getCategoryDocumentCounts().size());
        assertEquals(1, result.getCategorySendShares().size());
        assertEquals(9L, result.getCategorySendShares().get(0).getCount());
    }
}
