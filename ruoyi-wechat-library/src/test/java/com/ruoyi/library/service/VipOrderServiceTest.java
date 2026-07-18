package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.WechatPayProperties;
import com.ruoyi.library.domain.WlVipOrder;
import com.ruoyi.library.mapper.WlVipOrderMapper;
import com.ruoyi.library.payment.PaymentTransaction;
import java.time.Instant;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VipOrderServiceTest
{
    private WlVipOrderMapper orderMapper;
    private VipEntitlementService entitlementService;
    private VipOrderService service;
    private WlVipOrder order;

    @BeforeEach
    void setUp()
    {
        orderMapper = mock(WlVipOrderMapper.class);
        entitlementService = mock(VipEntitlementService.class);
        WechatPayProperties properties = new WechatPayProperties();
        properties.setAppId("wx-app");
        properties.setMchId("mch-1");
        order = new WlVipOrder();
        order.setId(7L);
        order.setUserId(1L);
        order.setPlanId(3L);
        order.setMerchantOrderNo("order-1");
        order.setPlanCodeSnapshot("MONTH");
        order.setPlanNameSnapshot("月卡");
        order.setAmountCent(990L);
        order.setCurrency("CNY");
        order.setValidDaysSnapshot(30);
        order.setGiftPointsSnapshot(10L);
        order.setOrderStatus("PREPAY_READY");
        when(orderMapper.selectByMerchantOrderNoForUpdate("order-1")).thenReturn(order);
        when(orderMapper.markPaid(any(), any(), any())).thenAnswer(invocation -> {
            order.setOrderStatus("PAID");
            return 1;
        });
        service = new VipOrderService(orderMapper, entitlementService, properties);
    }

    @Test
    void duplicatePaymentNotificationGrantsOnce()
    {
        PaymentTransaction transaction = transaction(990L);

        service.confirmPaid(transaction);
        service.confirmPaid(transaction);

        verify(orderMapper, times(1)).markPaid(7L, "wx-transaction-1", Date.from(transaction.getSuccessTime()));
        verify(entitlementService, times(1)).openOrRenew(org.mockito.ArgumentMatchers.eq(1L),
                argThat(plan -> plan.getValidDays() == 30 && plan.getGiftPoints() == 10L),
                org.mockito.ArgumentMatchers.eq("PAYMENT"), org.mockito.ArgumentMatchers.eq("order-1"));
        assertEquals("PAID", order.getOrderStatus());
    }

    @Test
    void mismatchedNotificationAmountDoesNotChangeOrder()
    {
        assertEquals("支付通知金额与订单不一致", assertThrows(ServiceException.class,
                () -> service.confirmPaid(transaction(991L))).getMessage());
        verify(orderMapper, never()).markPaid(any(), any(), any());
        verify(entitlementService, never()).openOrRenew(any(), any(), any(), any());
    }

    private PaymentTransaction transaction(long amount)
    {
        PaymentTransaction transaction = new PaymentTransaction();
        transaction.setAppId("wx-app");
        transaction.setMchId("mch-1");
        transaction.setOutTradeNo("order-1");
        transaction.setTransactionId("wx-transaction-1");
        transaction.setAmountCent(amount);
        transaction.setCurrency("CNY");
        transaction.setSuccessTime(Instant.parse("2026-07-18T08:00:00Z"));
        return transaction;
    }
}
