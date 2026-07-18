package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.WechatPayProperties;
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
import com.ruoyi.library.payment.WechatPayGateway;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 管理端会员全额退款和最终通知处理服务。 */
@Service
public class VipRefundService
{
    private final WlVipRefundMapper refundMapper; private final WlVipOrderMapper orderMapper;
    private final WlVipEntitlementMapper entitlementMapper; private final WlWxUserMapper userMapper;
    private final PointService pointService; private final WechatPayGateway gateway; private final WechatPayProperties properties;
    private final TransactionTemplate transactionTemplate;
    @Autowired
    public VipRefundService(WlVipRefundMapper refundMapper,WlVipOrderMapper orderMapper,WlVipEntitlementMapper entitlementMapper,
        WlWxUserMapper userMapper,PointService pointService,WechatPayGateway gateway,WechatPayProperties properties,
        PlatformTransactionManager transactionManager)
    {this.refundMapper=refundMapper;this.orderMapper=orderMapper;this.entitlementMapper=entitlementMapper;this.userMapper=userMapper;this.pointService=pointService;this.gateway=gateway;this.properties=properties;this.transactionTemplate=new TransactionTemplate(transactionManager);}
    VipRefundService(WlVipRefundMapper r,WlVipOrderMapper o,WlVipEntitlementMapper e,WlWxUserMapper u,PointService p)
    {this.refundMapper=r;this.orderMapper=o;this.entitlementMapper=e;this.userMapper=u;this.pointService=p;this.gateway=null;this.properties=null;this.transactionTemplate=null;}

    public WlVipRefund requestFullRefund(Long orderId,String reason,String confirmationToken,Long operatorId)
    {
        requireReason(reason); verifyConfirmationToken(confirmationToken);
        RefundContext context=transactionTemplate.execute(status->prepareRefund(orderId,reason,operatorId));
        if(context==null)throw new ServiceException("退款申请创建失败，请重试");
        if("SUCCESS".equals(context.refund.getRefundStatus())||"ACCEPTED".equals(context.refund.getRefundStatus()))return context.refund;
        String refundId=gateway.requestFullRefund(context.order,context.refund);
        return markAccepted(context.refund.getMerchantRefundNo(),refundId);
    }

    @Transactional
    public WlVipRefund markAccepted(String refundNo,String refundId)
    {
        if(transactionTemplate!=null)return transactionTemplate.execute(status->doMarkAccepted(refundNo,refundId));
        return doMarkAccepted(refundNo,refundId);
    }

    private WlVipRefund doMarkAccepted(String refundNo,String refundId)
    {
        WlVipRefund refund=requireRefund(refundNo); if("SUCCESS".equals(refund.getRefundStatus()))return refund;
        if(refundMapper.markAccepted(refund.getId(),refundId)!=1)throw new ServiceException("退款受理状态更新失败，请重试");
        refund.setRefundStatus("ACCEPTED");refund.setWechatRefundId(refundId);return refund;
    }

    private RefundContext prepareRefund(Long orderId,String reason,Long operatorId)
    {
        WlVipOrder order=orderMapper.selectByIdForUpdate(orderId); if(order==null)throw new ServiceException("会员订单不存在");
        WlVipRefund existing=refundMapper.selectByOrderId(orderId); if(existing!=null)return new RefundContext(order,existing);
        if(!"PAID".equals(order.getOrderStatus()))throw new ServiceException("只有已支付订单可以发起退款");
        WlVipRefund refund=new WlVipRefund();refund.setOrderId(orderId);refund.setUserId(order.getUserId());
        refund.setMerchantRefundNo("VREF"+UUID.randomUUID().toString().replace("-",""));refund.setRefundAmountCent(order.getAmountCent());
        refund.setRefundStatus("PROCESSING");refund.setShouldReclaimPoints(order.getGiftPointsSnapshot());refund.setOperatorId(operatorId);
        refund.setReason(reason.trim());refund.setCreateBy(String.valueOf(operatorId));
        if(refundMapper.insertRefund(refund)!=1||orderMapper.markRefundProcessing(orderId)!=1)throw new ServiceException("退款申请创建失败，请重试");
        return new RefundContext(order,refund);
    }
    @Transactional
    public RefundResult handleRefundNotification(Map<String,String> headers,String body)
    {
        RefundNotification notification=gateway.parseRefundNotification(headers,body);
        if("SUCCESS".equals(notification.getStatus()))return confirmSuccess(notification);
        WlVipRefund refund=requireRefund(notification.getMerchantRefundNo());
        if(!"SUCCESS".equals(refund.getRefundStatus()))
        {
            String reason="微信退款最终状态："+(notification.getStatus()==null?"未知":notification.getStatus());
            if(refundMapper.markFailed(refund.getId(),reason)!=1)throw new ServiceException("退款失败状态更新失败，请重试");
        }
        return new RefundResult(value(refund.getReclaimedPoints()),value(refund.getUnrecoveredPoints()));
    }

    @Transactional
    public RefundResult confirmSuccess(RefundNotification n)
    {
        if(n==null||!"SUCCESS".equals(n.getStatus())||n.getSuccessTime()==null)throw new ServiceException("退款通知状态不正确");
        WlVipRefund refund=requireRefund(n.getMerchantRefundNo());
        if("SUCCESS".equals(refund.getRefundStatus()))return new RefundResult(value(refund.getReclaimedPoints()),value(refund.getUnrecoveredPoints()));
        WlVipOrder order=orderMapper.selectByIdForUpdate(refund.getOrderId());if(order==null)throw new ServiceException("退款关联订单不存在");
        if(!order.getMerchantOrderNo().equals(n.getOutTradeNo()))throw new ServiceException("退款通知订单号不匹配");
        if(!refund.getRefundAmountCent().equals(n.getRefundAmountCent()))throw new ServiceException("退款通知金额不匹配");
        if(!"CNY".equals(n.getCurrency())||!"CNY".equals(order.getCurrency()))throw new ServiceException("退款通知币种不正确");
        WlWxUser user=userMapper.selectByIdForUpdate(refund.getUserId());if(user==null)throw new ServiceException("微信用户不存在");
        WlVipEntitlement removed=entitlementMapper.selectBySource("PAYMENT",order.getMerchantOrderNo());
        if(removed==null||!"ACTIVE".equals(removed.getStatus()))throw new ServiceException("支付会员权益不存在或已撤销");
        if(entitlementMapper.revokeById(removed.getId(),"refund")!=1)throw new ServiceException("支付会员权益撤销失败，请重试");
        Date expiry=replayExpiry(removed,entitlementMapper.selectActiveAfterId(refund.getUserId(),removed.getId()));
        if(userMapper.updateVipExpireTime(user.getId(),expiry,"refund")!=1)throw new ServiceException("会员到期时间重算失败，请重试");
        long expected=value(refund.getShouldReclaimPoints());long recovered=pointService.deductToFloorZero(user.getId(),expected,"会员退款追回赠送积分","REFUND:"+refund.getMerchantRefundNo());
        long shortfall=expected-recovered;
        if(refundMapper.markSuccess(refund.getId(),n.getWechatRefundId(),Date.from(n.getSuccessTime()),recovered,shortfall,"1")!=1||orderMapper.markRefunded(order.getId())!=1)
            throw new ServiceException("退款完成状态更新失败，请重试");
        return new RefundResult(recovered,shortfall);
    }
    public List<WlVipRefund> list(WlVipRefund q){return refundMapper.selectList(q==null?new WlVipRefund():q);}
    private WlVipRefund requireRefund(String no){if(no==null)throw new ServiceException("退款单号不能为空");WlVipRefund r=refundMapper.selectByMerchantRefundNoForUpdate(no);if(r==null)throw new ServiceException("退款记录不存在");return r;}
    private Date replayExpiry(WlVipEntitlement removed,List<WlVipEntitlement> later)
    {Instant expiry=removed.getOldExpireTime()==null?null:removed.getOldExpireTime().toInstant();for(WlVipEntitlement e:later){Instant created=e.getCreateTime()==null?e.getStartTime().toInstant():e.getCreateTime().toInstant();Instant start=expiry!=null&&expiry.isAfter(created)?expiry:created;expiry=start.plus(e.getGrantedDays(),ChronoUnit.DAYS);}return expiry==null?null:Date.from(expiry);}
    private void requireReason(String r){if(r==null||r.trim().isEmpty())throw new ServiceException("退款原因不能为空");if(r.trim().length()>500)throw new ServiceException("退款原因不能超过500个字符");}
    private void verifyConfirmationToken(String token){String expected=properties==null?null:properties.getRefundConfirmToken();if(expected==null||expected.isEmpty())throw new ServiceException("退款二次确认令牌未配置");byte[] a=expected.getBytes(StandardCharsets.UTF_8),b=(token==null?"":token).getBytes(StandardCharsets.UTF_8);if(!MessageDigest.isEqual(a,b))throw new ServiceException("退款二次确认令牌不正确");}
    private long value(Long v){return v==null?0L:v;}
    private static final class RefundContext
    {private final WlVipOrder order;private final WlVipRefund refund;private RefundContext(WlVipOrder o,WlVipRefund r){order=o;refund=r;}}
}
