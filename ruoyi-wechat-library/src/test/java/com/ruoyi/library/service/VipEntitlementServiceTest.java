package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.domain.WlVipEntitlement;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.mapper.WlVipEntitlementMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VipEntitlementServiceTest
{
    private WlVipEntitlementMapper entitlementMapper;
    private WlWxUserMapper userMapper;
    private PointService pointService;
    private VipEntitlementService service;
    private WlWxUser user;

    @BeforeEach
    void setUp()
    {
        entitlementMapper = mock(WlVipEntitlementMapper.class);
        userMapper = mock(WlWxUserMapper.class);
        pointService = mock(PointService.class);
        user = new WlWxUser();
        user.setId(1L);
        user.setStatus("0");
        user.setPointBalance(0L);
        when(userMapper.selectByIdForUpdate(1L)).thenAnswer(invocation -> user);
        when(userMapper.updateVipExpireTime(any(), any(), any())).thenAnswer(invocation -> {
            user.setVipExpireTime(invocation.getArgument(1));
            return 1;
        });
        when(entitlementMapper.insertEntitlement(any(WlVipEntitlement.class))).thenAnswer(invocation -> {
            WlVipEntitlement value = invocation.getArgument(0);
            value.setId(100L);
            return 1;
        });
        WlPointRecord pointRecord = new WlPointRecord();
        pointRecord.setId(88L);
        when(pointService.creditFixedAfterLock(any(), any(), any(), any())).thenReturn(pointRecord);
        service = new VipEntitlementService(entitlementMapper, userMapper, pointService,
                Clock.fixed(Instant.parse("2026-07-16T00:00:00Z"), ZoneOffset.UTC));
    }

    @Test
    void monthAndYearRenewFromCurrentExpiryAndGiftOnce()
    {
        service.openOrRenew(1L, plan(30, 20L), "PAYMENT", "order-1");
        service.openOrRenew(1L, plan(365, 50L), "PAYMENT", "order-2");

        assertEquals("2027-08-15T00:00:00Z", user.getVipExpireTime().toInstant().toString());
        verify(pointService, times(1)).creditFixedAfterLock(user, 20L, "VIP_GIFT:order-1", "会员套餐赠送积分");
        verify(pointService, times(1)).creditFixedAfterLock(user, 50L, "VIP_GIFT:order-2", "会员套餐赠送积分");
    }

    @Test
    void customPlanExtendsMembershipByConfiguredDays()
    {
        service.openOrRenew(1L, plan(90, 0L), "PAYMENT", "custom-90");

        assertEquals("2026-10-14T00:00:00Z", user.getVipExpireTime().toInstant().toString());
    }

    @Test
    void planValidDaysOutsideConfiguredRangeAreRejected()
    {
        assertPlanDaysMessage(plan(0, 0L));
        assertPlanDaysMessage(plan(3651, 0L));
    }

    @Test
    void duplicateSourceReturnsExistingEntitlementWithoutSecondGift()
    {
        WlVipEntitlement existing = new WlVipEntitlement();
        existing.setId(9L);
        existing.setUserId(1L);
        existing.setSourceType("PAYMENT");
        existing.setSourceBizNo("order-1");
        when(entitlementMapper.selectBySource("PAYMENT", "order-1")).thenReturn(existing);

        assertSame(existing, service.openOrRenew(1L, plan(30, 20L), "PAYMENT", "order-1"));
        verify(userMapper, never()).selectByIdForUpdate(any());
        verify(pointService, never()).creditFixedAfterLock(any(), any(), any(), any());
    }

    @Test
    void compensationExtendsWithoutGiftPointsAndRecordsAuditSnapshot()
    {
        service.compensate(1L, 7, 99L, "系统故障补偿", "compensation-1");

        ArgumentCaptor<WlVipEntitlement> captor = ArgumentCaptor.forClass(WlVipEntitlement.class);
        verify(entitlementMapper).insertEntitlement(captor.capture());
        WlVipEntitlement entitlement = captor.getValue();
        assertEquals("COMPENSATION", entitlement.getSourceType());
        assertEquals(0L, entitlement.getGiftPoints());
        assertEquals(Long.valueOf(99L), entitlement.getOperatorId());
        assertEquals("系统故障补偿", entitlement.getReason());
        assertEquals("2026-07-23T00:00:00Z", entitlement.getNewExpireTime().toInstant().toString());
        verify(pointService, never()).creditFixedAfterLock(any(), any(), any(), any());
    }

    private WlVipPlan plan(int validDays, long giftPoints)
    {
        WlVipPlan plan = new WlVipPlan();
        plan.setId(3L);
        plan.setPlanCode("PLAN_" + validDays);
        plan.setPlanName(validDays + "天套餐");
        plan.setPriceCent(990L);
        plan.setValidDays(validDays);
        plan.setGiftPoints(giftPoints);
        plan.setStatus("0");
        return plan;
    }

    private void assertPlanDaysMessage(WlVipPlan plan)
    {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.openOrRenew(1L, plan, "PAYMENT", "invalid-" + plan.getValidDays()));
        assertEquals("会员套餐有效天数必须在1到3650天之间", exception.getMessage());
    }
}
