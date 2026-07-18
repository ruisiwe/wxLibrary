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
import java.util.Date;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 会员订单创建、预支付和支付通知状态机。 */
@Service
public class VipOrderService
{
    private final WlVipOrderMapper orderMapper; private final VipEntitlementService entitlementService;
    private final WechatPayProperties properties; private final WechatPayGateway gateway;
    private final VipPlanService planService; private final WlWxUserMapper userMapper;

    @Autowired
    public VipOrderService(WlVipOrderMapper orderMapper, VipEntitlementService entitlementService,
            WechatPayProperties properties, WechatPayGateway gateway, VipPlanService planService,
            WlWxUserMapper userMapper)
    { this.orderMapper=orderMapper;this.entitlementService=entitlementService;this.properties=properties;
      this.gateway=gateway;this.planService=planService;this.userMapper=userMapper; }

    VipOrderService(WlVipOrderMapper orderMapper, VipEntitlementService entitlementService,
            WechatPayProperties properties)
    { this(orderMapper,entitlementService,properties,null,null,null); }

    public Map<String,String> createPrepay(Long userId, Long planId)
    {
        WlWxUser user=userMapper.selectById(userId);
        if(user==null) throw new ServiceException("微信用户不存在");
        if(!"0".equals(user.getStatus())) throw new ServiceException("当前账号已停用，请联系管理员");
        WlVipPlan plan=planService.getEnabled(planId);
        WlVipOrder order=new WlVipOrder(); order.setUserId(userId);order.setPlanId(plan.getId());
        order.setMerchantOrderNo("VIP"+UUID.randomUUID().toString().replace("-",""));
        order.setPlanCodeSnapshot(plan.getPlanCode());order.setPlanNameSnapshot(plan.getPlanName());
        order.setAmountCent(plan.getPriceCent());order.setCurrency("CNY");order.setValidDaysSnapshot(plan.getValidDays());
        order.setGiftPointsSnapshot(plan.getGiftPoints());order.setOrderStatus("CREATED");order.setCreateBy(String.valueOf(userId));
        if(orderMapper.insertOrder(order)!=1) throw new ServiceException("会员订单创建失败，请重试");
        Map<String,String> prepay=new LinkedHashMap<>(gateway.createJsapiPrepay(order,user.getOpenid()));
        if(orderMapper.markPrepayReady(order.getId())!=1) throw new ServiceException("会员订单预支付状态更新失败，请重试");
        prepay.put("merchantOrderNo", order.getMerchantOrderNo());
        order.setOrderStatus("PREPAY_READY"); return prepay;
    }

    @Transactional
    public void handlePaymentNotification(Map<String,String> headers,String body)
    { confirmPaid(gateway.parsePaymentNotification(headers,body)); }

    @Transactional
    public WlVipOrder confirmPaid(PaymentTransaction transaction)
    {
        validateTransaction(transaction);
        WlVipOrder order=orderMapper.selectByMerchantOrderNoForUpdate(transaction.getOutTradeNo());
        if(order==null) throw new ServiceException("支付订单不存在");
        verifyTransaction(order,transaction);
        if("PAID".equals(order.getOrderStatus())||"REFUND_PROCESSING".equals(order.getOrderStatus())||"REFUNDED".equals(order.getOrderStatus())) return order;
        if(!"CREATED".equals(order.getOrderStatus())&&!"PREPAY_READY".equals(order.getOrderStatus()))
            throw new ServiceException("支付订单当前状态不允许付款");
        Date paidTime=Date.from(transaction.getSuccessTime());
        if(orderMapper.markPaid(order.getId(),transaction.getTransactionId(),paidTime)!=1)
            throw new ServiceException("支付订单状态已变化，请重试");
        entitlementService.openOrRenew(order.getUserId(),order.toPlanSnapshot(),"PAYMENT",order.getMerchantOrderNo());
        order.setOrderStatus("PAID");order.setWechatTransactionId(transaction.getTransactionId());order.setPaidTime(paidTime);
        return order;
    }

    public List<WlVipOrder> list(WlVipOrder query){return orderMapper.selectList(query==null?new WlVipOrder():query);}
    public WlVipOrder get(Long id){WlVipOrder o=orderMapper.selectById(id);if(o==null)throw new ServiceException("会员订单不存在");return o;}
    public WlVipOrder getForUser(Long userId, String merchantOrderNo)
    {
        if (userId == null || merchantOrderNo == null || merchantOrderNo.trim().isEmpty())
            throw new ServiceException("会员订单不存在");
        WlVipOrder order = orderMapper.selectByMerchantOrderNo(merchantOrderNo.trim());
        if (order == null || !userId.equals(order.getUserId()))
            throw new ServiceException("会员订单不存在");
        return order;
    }

    private void validateTransaction(PaymentTransaction t)
    { if(t==null||t.getOutTradeNo()==null||t.getTransactionId()==null||t.getAmountCent()==null||t.getSuccessTime()==null)
        throw new ServiceException("支付通知内容不完整"); }
    private void verifyTransaction(WlVipOrder o,PaymentTransaction t)
    {
        if(!safe(properties.getAppId()).equals(t.getAppId())) throw new ServiceException("支付通知应用编号不匹配");
        if(!safe(properties.getMchId()).equals(t.getMchId())) throw new ServiceException("支付通知商户号不匹配");
        if(!o.getAmountCent().equals(t.getAmountCent())) throw new ServiceException("支付通知金额与订单不一致");
        if(!"CNY".equals(t.getCurrency())||!"CNY".equals(o.getCurrency())) throw new ServiceException("支付通知币种不正确");
    }
    private String safe(String s){return s==null?"":s;}
}
