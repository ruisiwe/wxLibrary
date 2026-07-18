package com.ruoyi.library.payment;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.WechatPayProperties;
import com.ruoyi.library.domain.WlVipOrder;
import com.ruoyi.library.domain.WlVipRefund;
import com.wechat.pay.java.core.RSAAutoCertificateConfig;
import com.wechat.pay.java.core.notification.NotificationParser;
import com.wechat.pay.java.core.notification.RequestParam;
import com.wechat.pay.java.service.payments.jsapi.JsapiServiceExtension;
import com.wechat.pay.java.service.payments.jsapi.model.Payer;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayRequest;
import com.wechat.pay.java.service.payments.jsapi.model.PrepayWithRequestPaymentResponse;
import com.wechat.pay.java.service.payments.model.Transaction;
import com.wechat.pay.java.service.refund.RefundService;
import com.wechat.pay.java.service.refund.model.AmountReq;
import com.wechat.pay.java.service.refund.model.CreateRequest;
import com.wechat.pay.java.service.refund.model.Refund;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

/** 使用微信支付官方 SDK 完成 JSAPI 下单、通知验签解密和全额退款。 */
@Component
public class OfficialWechatPayGateway implements WechatPayGateway
{
    private final WechatPayProperties properties;
    private volatile Clients clients;

    public OfficialWechatPayGateway(WechatPayProperties properties) { this.properties = properties; }

    @Override
    public Map<String, String> createJsapiPrepay(WlVipOrder order, String openid)
    {
        requireText(openid, "微信用户标识不能为空");
        try
        {
            PrepayRequest request = new PrepayRequest();
            request.setAppid(require(properties.getAppId(), "微信支付应用编号未配置"));
            request.setMchid(require(properties.getMchId(), "微信支付商户号未配置"));
            request.setDescription(safeDescription(order.getPlanNameSnapshot()));
            request.setOutTradeNo(order.getMerchantOrderNo());
            request.setNotifyUrl(require(properties.getNotifyUrl(), "微信支付通知地址未配置"));
            com.wechat.pay.java.service.payments.jsapi.model.Amount amount =
                    new com.wechat.pay.java.service.payments.jsapi.model.Amount();
            amount.setTotal(Math.toIntExact(order.getAmountCent()));
            amount.setCurrency("CNY");
            request.setAmount(amount);
            Payer payer = new Payer(); payer.setOpenid(openid); request.setPayer(payer);
            PrepayWithRequestPaymentResponse response = clients().jsapi.prepayWithRequestPayment(request);
            Map<String, String> result = new LinkedHashMap<>();
            result.put("appId", response.getAppId()); result.put("timeStamp", response.getTimeStamp());
            result.put("nonceStr", response.getNonceStr()); result.put("package", response.getPackageVal());
            result.put("signType", response.getSignType()); result.put("paySign", response.getPaySign());
            return result;
        }
        catch (ServiceException exception) { throw exception; }
        catch (RuntimeException exception) { throw new ServiceException("微信支付下单失败，请稍后重试"); }
    }

    @Override
    public PaymentTransaction parsePaymentNotification(Map<String, String> headers, String body)
    {
        try
        {
            Transaction source = clients().parser.parse(request(headers, body), Transaction.class);
            if (source.getTradeState() != Transaction.TradeStateEnum.SUCCESS)
                throw new ServiceException("支付通知交易状态不是成功");
            PaymentTransaction result = new PaymentTransaction(); result.setAppId(source.getAppid());
            result.setMchId(source.getMchid()); result.setOutTradeNo(source.getOutTradeNo());
            result.setTransactionId(source.getTransactionId());
            if (source.getAmount() != null)
            { result.setAmountCent(source.getAmount().getTotal().longValue()); result.setCurrency(source.getAmount().getCurrency()); }
            result.setSuccessTime(parseTime(source.getSuccessTime(), "支付成功时间不正确")); return result;
        }
        catch (ServiceException exception) { throw exception; }
        catch (RuntimeException exception) { throw new ServiceException("支付通知验签或解密失败"); }
    }

    @Override
    public String requestFullRefund(WlVipOrder order, WlVipRefund refund)
    {
        try
        {
            CreateRequest request = new CreateRequest(); request.setOutTradeNo(order.getMerchantOrderNo());
            request.setOutRefundNo(refund.getMerchantRefundNo()); request.setReason(refund.getReason());
            request.setNotifyUrl(require(properties.getRefundNotifyUrl(), "微信退款通知地址未配置"));
            AmountReq amount = new AmountReq(); amount.setRefund(order.getAmountCent());
            amount.setTotal(order.getAmountCent()); amount.setCurrency("CNY"); request.setAmount(amount);
            Refund result = clients().refund.create(request);
            if (result == null || result.getRefundId() == null) throw new ServiceException("微信退款受理结果不完整");
            return result.getRefundId();
        }
        catch (ServiceException exception) { throw exception; }
        catch (RuntimeException exception) { throw new ServiceException("微信退款申请失败，请稍后重试"); }
    }

    @Override
    public RefundNotification parseRefundNotification(Map<String, String> headers, String body)
    {
        try
        {
            com.wechat.pay.java.service.refund.model.RefundNotification source = clients().parser.parse(
                    request(headers, body), com.wechat.pay.java.service.refund.model.RefundNotification.class);
            RefundNotification result = new RefundNotification(); result.setMerchantRefundNo(source.getOutRefundNo());
            result.setWechatRefundId(source.getRefundId()); result.setOutTradeNo(source.getOutTradeNo());
            result.setStatus(source.getRefundStatus() == null ? null : source.getRefundStatus().name());
            if (source.getAmount() != null)
            { result.setRefundAmountCent(source.getAmount().getRefund()); result.setCurrency(source.getAmount().getCurrency()); }
            if (source.getSuccessTime() != null) result.setSuccessTime(parseTime(source.getSuccessTime(), "退款成功时间不正确"));
            return result;
        }
        catch (ServiceException exception) { throw exception; }
        catch (RuntimeException exception) { throw new ServiceException("退款通知验签或解密失败"); }
    }

    private Clients clients()
    {
        Clients current = clients; if (current != null) return current;
        synchronized (this)
        {
            if (clients == null)
            {
                RSAAutoCertificateConfig config = new RSAAutoCertificateConfig.Builder()
                        .merchantId(require(properties.getMchId(), "微信支付商户号未配置"))
                        .privateKeyFromPath(require(properties.getPrivateKeyPath(), "微信支付商户私钥路径未配置"))
                        .merchantSerialNumber(require(properties.getMerchantSerialNumber(), "微信支付商户证书序列号未配置"))
                        .apiV3Key(require(properties.getApiV3Key(), "微信支付 APIv3 密钥未配置")).build();
                clients = new Clients(new JsapiServiceExtension.Builder().config(config).build(),
                        new RefundService.Builder().config(config).build(), new NotificationParser(config));
            }
            return clients;
        }
    }

    private RequestParam request(Map<String, String> h, String body)
    {
        return new RequestParam.Builder().serialNumber(header(h, "Wechatpay-Serial"))
                .timestamp(header(h, "Wechatpay-Timestamp")).nonce(header(h, "Wechatpay-Nonce"))
                .signature(header(h, "Wechatpay-Signature")).body(body == null ? "" : body).build();
    }
    private String header(Map<String,String> headers,String name)
    { if(headers!=null)for(Map.Entry<String,String> e:headers.entrySet())if(name.equalsIgnoreCase(e.getKey()))return require(e.getValue(),"微信支付通知请求头不完整");throw new ServiceException("微信支付通知请求头不完整"); }
    private java.time.Instant parseTime(String value,String message){try{return OffsetDateTime.parse(value).toInstant();}catch(RuntimeException e){throw new ServiceException(message);}}
    private String safeDescription(String value){String v=value==null?"会员套餐":value.trim();return v.length()<=127?v:v.substring(0,127);}
    private String require(String value,String message){requireText(value,message);return value.trim();}
    private void requireText(String value,String message){if(value==null||value.trim().isEmpty())throw new ServiceException(message);}
    private static final class Clients
    {private final JsapiServiceExtension jsapi;private final RefundService refund;private final NotificationParser parser;private Clients(JsapiServiceExtension j,RefundService r,NotificationParser p){jsapi=j;refund=r;parser=p;}}
}
