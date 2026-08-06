package com.ruoyi.library.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.ruoyi.library.dto.DashboardCategoryCount;
import com.ruoyi.library.dto.DashboardDailyCount;
import com.ruoyi.library.dto.DashboardMonthlyData;
import com.ruoyi.library.dto.DashboardMonthlySeries;
import com.ruoyi.library.dto.DashboardPeriodCategoryCount;
import com.ruoyi.library.dto.DashboardSummary;
import com.ruoyi.library.dto.DashboardTrendData;
import com.ruoyi.library.dto.LibraryDashboardData;
import com.ruoyi.library.mapper.LibraryDashboardMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/** 组装后台首页全部文库统计数据。 */
@Service
public class LibraryDashboardService
{
    private static final DateTimeFormatter DAY_FORMAT = DateTimeFormatter.ISO_LOCAL_DATE;
    private static final DateTimeFormatter MONTH_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM");
    private final LibraryDashboardMapper mapper;
    private final Clock clock;

    @Autowired
    public LibraryDashboardService(LibraryDashboardMapper mapper)
    {
        this(mapper, Clock.systemDefaultZone());
    }

    LibraryDashboardService(LibraryDashboardMapper mapper, Clock clock)
    {
        this.mapper = mapper;
        this.clock = clock;
    }

    /** 查询并补齐后台首页所需的全部统计区间。 */
    public LibraryDashboardData load()
    {
        ZoneId zone = clock.getZone();
        LocalDate today = LocalDate.now(clock);
        LocalDate dayStart = today.minusDays(6);
        LocalDate dayEnd = today.plusDays(1);
        LocalDate monthStart = today.withDayOfMonth(1).minusMonths(11);
        LocalDate monthEnd = today.withDayOfMonth(1).plusMonths(1);

        DashboardSummary summary = mapper.selectSummary(new Date(clock.millis()));
        if (summary == null) summary = new DashboardSummary(0L, 0L, 0L, 0L);
        List<DashboardCategoryCount> categories = safeCategories(mapper.selectCategoryDocumentCounts());
        DashboardMonthlyData monthly = buildMonthly(monthStart, monthEnd, categories,
                mapper.selectMonthlyPaidExchangeCounts(atStart(monthStart, zone), atStart(monthEnd, zone)));
        DashboardTrendData trend = buildTrend(dayStart, dayEnd,
                mapper.selectDailyPaidExchangeCounts(atStart(dayStart, zone), atStart(dayEnd, zone)),
                mapper.selectDailyActiveUserCounts(dayStart.format(DAY_FORMAT), dayEnd.format(DAY_FORMAT)));
        List<DashboardCategoryCount> sends = safeCategories(mapper.selectCategorySendCounts());
        return new LibraryDashboardData(summary, monthly, trend, categories, sends);
    }

    private DashboardMonthlyData buildMonthly(LocalDate start, LocalDate end,
            List<DashboardCategoryCount> categories, List<DashboardPeriodCategoryCount> rows)
    {
        List<String> months = new ArrayList<>();
        for (YearMonth month = YearMonth.from(start); month.isBefore(YearMonth.from(end));
                month = month.plusMonths(1))
        {
            months.add(month.format(MONTH_FORMAT));
        }

        Map<Long, Map<String, Long>> values = new HashMap<>();
        if (rows != null)
        {
            for (DashboardPeriodCategoryCount row : rows)
            {
                if (row == null || row.getCategoryId() == null || row.getPeriod() == null) continue;
                Map<String, Long> categoryValues = values.get(row.getCategoryId());
                if (categoryValues == null)
                {
                    categoryValues = new HashMap<>();
                    values.put(row.getCategoryId(), categoryValues);
                }
                categoryValues.put(row.getPeriod(), count(row.getCount()));
            }
        }

        List<DashboardMonthlySeries> series = new ArrayList<>();
        for (DashboardCategoryCount category : categories)
        {
            Map<String, Long> categoryValues = values.get(category.getCategoryId());
            if (categoryValues == null) categoryValues = Collections.emptyMap();
            List<Long> monthValues = new ArrayList<>();
            for (String month : months) monthValues.add(count(categoryValues.get(month)));
            series.add(new DashboardMonthlySeries(category.getCategoryId(),
                    category.getCategoryName(), monthValues));
        }
        return new DashboardMonthlyData(months, series);
    }

    private DashboardTrendData buildTrend(LocalDate start, LocalDate end,
            List<DashboardDailyCount> paidRows, List<DashboardDailyCount> activeRows)
    {
        Map<String, Long> paid = dailyMap(paidRows);
        Map<String, Long> active = dailyMap(activeRows);
        List<String> dates = new ArrayList<>();
        List<Long> paidValues = new ArrayList<>();
        List<Long> activeValues = new ArrayList<>();
        for (LocalDate date = start; date.isBefore(end); date = date.plusDays(1))
        {
            String key = date.format(DAY_FORMAT);
            dates.add(key);
            paidValues.add(count(paid.get(key)));
            activeValues.add(count(active.get(key)));
        }
        return new DashboardTrendData(dates, paidValues, activeValues);
    }

    private Map<String, Long> dailyMap(List<DashboardDailyCount> rows)
    {
        Map<String, Long> values = new LinkedHashMap<>();
        if (rows == null) return values;
        for (DashboardDailyCount row : rows)
        {
            if (row != null && row.getDate() != null)
                values.put(row.getDate(), count(row.getCount()));
        }
        return values;
    }

    private List<DashboardCategoryCount> safeCategories(List<DashboardCategoryCount> values)
    {
        return values == null ? new ArrayList<DashboardCategoryCount>() : values;
    }

    private Date atStart(LocalDate date, ZoneId zone)
    {
        return Date.from(date.atStartOfDay(zone).toInstant());
    }

    private long count(Long value)
    {
        return value == null ? 0L : value;
    }
}
