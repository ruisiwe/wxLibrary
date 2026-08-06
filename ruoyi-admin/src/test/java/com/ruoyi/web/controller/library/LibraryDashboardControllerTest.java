package com.ruoyi.web.controller.library;

import java.util.ArrayList;
import com.ruoyi.library.dto.DashboardCategoryCount;
import com.ruoyi.library.dto.DashboardMonthlyData;
import com.ruoyi.library.dto.DashboardSummary;
import com.ruoyi.library.dto.DashboardTrendData;
import com.ruoyi.library.dto.LibraryDashboardData;
import com.ruoyi.library.service.LibraryDashboardService;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LibraryDashboardControllerTest
{
    @Test
    void returnsAllDashboardSections() throws Exception
    {
        LibraryDashboardService service = mock(LibraryDashboardService.class);
        LibraryDashboardData data = new LibraryDashboardData(
                new DashboardSummary(10L, 3L, 20L, 8L),
                DashboardMonthlyData.empty(), DashboardTrendData.empty(),
                new ArrayList<DashboardCategoryCount>(),
                new ArrayList<DashboardCategoryCount>());
        when(service.load()).thenReturn(data);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new LibraryDashboardController(service)).build();

        mockMvc.perform(get("/library/dashboard"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.summary.userCount").value(10))
                .andExpect(jsonPath("$.data.summary.memberCount").value(3))
                .andExpect(jsonPath("$.data.summary.documentCount").value(20))
                .andExpect(jsonPath("$.data.summary.paidDocumentCount").value(8))
                .andExpect(jsonPath("$.data.monthlyPaidExchanges.months").isArray())
                .andExpect(jsonPath("$.data.sevenDayTrend.dates").isArray())
                .andExpect(jsonPath("$.data.categoryDocumentCounts").isArray())
                .andExpect(jsonPath("$.data.categorySendShares").isArray());
    }
}
