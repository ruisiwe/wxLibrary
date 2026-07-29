package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.mapper.WlVipPlanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class VipPlanServiceTest
{
    private WlVipPlanMapper planMapper;
    private VipPlanService service;

    @BeforeEach
    void setUp()
    {
        planMapper = mock(WlVipPlanMapper.class);
        service = new VipPlanService(planMapper);
    }

    @Test
    void addAcceptsCustomAndBoundaryValidDays()
    {
        WlVipPlan oneDay = plan(1);
        WlVipPlan custom = plan(90);
        WlVipPlan tenYears = plan(3650);

        service.add(oneDay, "admin");
        service.add(custom, "admin");
        service.add(tenYears, "admin");

        verify(planMapper).insertPlan(oneDay);
        verify(planMapper).insertPlan(custom);
        verify(planMapper).insertPlan(tenYears);
    }

    @Test
    void addRejectsValidDaysOutsideConfiguredRange()
    {
        assertValidDaysMessage(plan(null));
        assertValidDaysMessage(plan(0));
        assertValidDaysMessage(plan(-1));
        assertValidDaysMessage(plan(3651));
    }

    private void assertValidDaysMessage(WlVipPlan plan)
    {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.add(plan, "admin"));
        assertEquals("会员套餐有效天数必须在1到3650天之间", exception.getMessage());
    }

    private WlVipPlan plan(Integer validDays)
    {
        WlVipPlan plan = new WlVipPlan();
        plan.setPlanCode("PLAN_" + validDays);
        plan.setPlanName("测试套餐");
        plan.setPriceCent(990L);
        plan.setValidDays(validDays);
        plan.setGiftPoints(0L);
        plan.setSortOrder(0);
        plan.setStatus("0");
        return plan;
    }
}
