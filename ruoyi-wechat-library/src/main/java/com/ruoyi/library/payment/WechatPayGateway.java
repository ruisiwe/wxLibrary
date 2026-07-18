package com.ruoyi.library.payment;

import com.ruoyi.library.domain.WlVipOrder;
import com.ruoyi.library.domain.WlVipRefund;
import java.util.Map;

/** 微信支付官方 SDK 的唯一边界，控制器不得自行验签或组装签名。 */
public interface WechatPayGateway
{
    Map<String, String> createJsapiPrepay(WlVipOrder order, String openid);
    PaymentTransaction parsePaymentNotification(Map<String, String> headers, String body);
    String requestFullRefund(WlVipOrder order, WlVipRefund refund);
    RefundNotification parseRefundNotification(Map<String, String> headers, String body);
}
