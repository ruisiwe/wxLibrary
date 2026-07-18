package com.ruoyi.library.service;

import com.ruoyi.library.domain.WlVipEntitlement;
import com.ruoyi.library.domain.WlVipOrder;
import com.ruoyi.library.domain.WlVipRefund;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.RefundResult;
import com.ruoyi.library.mapper.WlVipEntitlementMapper;
import com.ruoyi.library.mapper.WlVipOrderMapper;
import com.ruoyi.library.mapper.WlVipRefundMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.payment.RefundNotification;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VipRefundServiceTest
{
    private WlVipRefundMapper refundMapper;
    private WlVipOrderMapper orderMapper;
    private WlVipEntitlementMapper entitlementMapper;
    private WlWxUserMapper userMapper;
    private PointService pointService;
    private VipRefundService service;
    private WlVipRefund refund;
    private WlVipOrder order;
    private WlVipEntitlement entitlement;

    @BeforeEach
    void setUp()
    {
        refundMapper = mock(WlVipRefundMapper.class);
        orderMapper = mock(WlVipOrderMapper.class);
        entitlementMapper = mock(WlVipEntitlementMapper.class);
        userMapper = mock(WlWxUserMapper.class);
        pointService = mock(PointService.class);
        refund = new WlVipRefund();
        refund.setId(8L);
        refund.setOrderId(7L);
        refund.setUserId(1L);
        refund.setMerchantRefundNo("refund-1");
        refund.setRefundAmountCent(990L);
        refund.setRefundStatus("ACCEPTED");
        refund.setShouldReclaimPoints(10L);
        order = new WlVipOrder();
        order.setId(7L);
        order.setUserId(1L);
        order.setMerchantOrderNo("order-1");
        order.setAmountCent(990L);
        order.setCurrency("CNY");
        order.setOrderStatus("REFUND_PROCESSING");
        entitlement = new WlVipEntitlement();
        entitlement.setId(6L);
        entitlement.setUserId(1L);
        entitlement.setSourceType("PAYMENT");
        entitlement.setSourceBizNo("order-1");
        entitlement.setStatus("ACTIVE");
        entitlement.setOldExpireTime(Date.from(Instant.parse("2026-07-16T00:00:00Z")));
        when(refundMapper.selectByMerchantRefundNoForUpdate("refund-1")).thenReturn(refund);
        when(orderMapper.selectByIdForUpdate(7L)).thenReturn(order);
        when(entitlementMapper.selectBySource("PAYMENT", "order-1")).thenReturn(entitlement);
        when(entitlementMapper.selectActiveAfterId(1L, 6L)).thenReturn(Collections.emptyList());
        WlWxUser user = new WlWxUser();
        user.setId(1L);
        user.setStatus("0");
        user.setPointBalance(3L);
        when(userMapper.selectByIdForUpdate(1L)).thenReturn(user);
        when(userMapper.updateVipExpireTime(any(), any(), any())).thenReturn(1);
        when(entitlementMapper.revokeById(6L, "refund")).thenReturn(1);
        when(pointService.deductToFloorZero(1L, 10L, "会员退款追回赠送积分", "REFUND:refund-1"))
                .thenReturn(3L);
        when(refundMapper.markSuccess(any(), any(), any(), any(), any(), any())).thenReturn(1);
        when(refundMapper.markAccepted(8L, "wx-refund-1")).thenReturn(1);
        when(orderMapper.markRefunded(7L)).thenReturn(1);
        service = new VipRefundService(refundMapper, orderMapper, entitlementMapper, userMapper, pointService);
    }

    @Test
    void acceptedRefundDoesNotRevokeUntilFinalSuccess()
    {
        service.markAccepted("refund-1", "wx-refund-1");
        verify(entitlementMapper, never()).revokeById(any(), any());
    }

    @Test
    void finalSuccessRevokesAndPointRecoveryStopsAtZeroWithShortfall()
    {
        RefundResult result = service.confirmSuccess(notification());

        verify(entitlementMapper).revokeById(6L, "refund");
        assertEquals(3L, result.getRecoveredPoints());
        assertEquals(7L, result.getUnrecoveredPoints());
        verify(refundMapper).markSuccess(8L, "wx-refund-1", Date.from(notification().getSuccessTime()),
                3L, 7L, "1");
    }

    private RefundNotification notification()
    {
        RefundNotification notification = new RefundNotification();
        notification.setMerchantRefundNo("refund-1");
        notification.setWechatRefundId("wx-refund-1");
        notification.setOutTradeNo("order-1");
        notification.setRefundAmountCent(990L);
        notification.setCurrency("CNY");
        notification.setStatus("SUCCESS");
        notification.setSuccessTime(Instant.parse("2026-07-18T09:00:00Z"));
        return notification;
    }
}
