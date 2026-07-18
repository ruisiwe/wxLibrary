package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.WechatPayProperties;
import com.ruoyi.library.domain.WlVipOrder;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.mapper.WlVipOrderMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.payment.PaymentTransaction;
import com.ruoyi.library.payment.WechatPayGateway;
import java.time.Instant;
import java.util.Date;
import java.util.Collections;
import java.util.Map;
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

    @Test
    void userCanOnlyQueryOwnOrderStatus()
    {
        when(orderMapper.selectByMerchantOrderNo("order-1")).thenReturn(order);

        assertEquals("PREPAY_READY", service.getForUser(1L, "order-1").getOrderStatus());
        assertEquals("会员订单不存在", assertThrows(ServiceException.class,
                () -> service.getForUser(2L, "order-1")).getMessage());
    }

    @Test
    void prepayResultIncludesMerchantOrderNumberForStatusQuery()
    {
        WechatPayGateway gateway = mock(WechatPayGateway.class);
        VipPlanService planService = mock(VipPlanService.class);
        WlWxUserMapper userMapper = mock(WlWxUserMapper.class);
        WlWxUser user = new WlWxUser();
        user.setId(1L); user.setOpenid("openid-1"); user.setStatus("0");
        WlVipPlan plan = new WlVipPlan();
        plan.setId(3L); plan.setPlanCode("MONTH"); plan.setPlanName("月卡");
        plan.setPriceCent(990L); plan.setValidDays(30); plan.setGiftPoints(10L);
        when(userMapper.selectById(1L)).thenReturn(user);
        when(planService.getEnabled(3L)).thenReturn(plan);
        when(orderMapper.insertOrder(any())).thenAnswer(invocation -> {
            ((WlVipOrder) invocation.getArgument(0)).setId(9L);
            return 1;
        });
        when(gateway.createJsapiPrepay(any(), org.mockito.ArgumentMatchers.eq("openid-1")))
                .thenReturn(Collections.singletonMap("paySign", "sign"));
        when(orderMapper.markPrepayReady(9L)).thenReturn(1);
        VipOrderService createService = new VipOrderService(orderMapper, entitlementService,
                new WechatPayProperties(), gateway, planService, userMapper);

        Map<String, String> result = createService.createPrepay(1L, 3L);

        assertEquals("sign", result.get("paySign"));
        org.junit.jupiter.api.Assertions.assertTrue(result.get("merchantOrderNo").startsWith("VIP"));
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
