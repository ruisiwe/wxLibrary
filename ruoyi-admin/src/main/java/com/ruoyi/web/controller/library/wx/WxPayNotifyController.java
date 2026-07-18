package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.service.VipOrderService;
import com.ruoyi.library.service.VipRefundService;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.ExceptionHandler;

/** 微信支付和退款通知接口，仅将原始报文交给官方 SDK 边界验签。 */
@RestController
@RequestMapping("/wx/pay/notify")
public class WxPayNotifyController
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WxPayNotifyController.class);
    private final VipOrderService orderService; private final VipRefundService refundService;
    public WxPayNotifyController(VipOrderService o,VipRefundService r){orderService=o;refundService=r;}
    /** 接收支付结果通知，已验签的重复通知直接返回成功。 */
    @PostMapping("/payment") public Map<String,String> payment(@RequestHeader HttpHeaders headers,@RequestBody String body){orderService.handlePaymentNotification(flatten(headers),body);return success();}
    /** 接收退款最终结果通知，只有成功状态才撤销权益。 */
    @PostMapping("/refund") public Map<String,String> refund(@RequestHeader HttpHeaders headers,@RequestBody String body){refundService.handleRefundNotification(flatten(headers),body);return success();}
    /** 通知处理失败时返回微信支付约定的失败结构，以便平台重试。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> notifyFailure(Exception exception)
    {
        LOGGER.error("微信支付通知处理失败", exception);
        Map<String, String> result = new java.util.HashMap<>();
        result.put("code", "FAIL");
        result.put("message", "通知处理失败，请稍后重试");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
    }
    private Map<String,String> flatten(HttpHeaders h){Map<String,String> result=new java.util.HashMap<>();for(Map.Entry<String,java.util.List<String>> e:h.entrySet())if(!e.getValue().isEmpty())result.put(e.getKey(),e.getValue().get(0));return result;}
    private Map<String,String> success(){Map<String,String> r=new java.util.HashMap<>();r.put("code","SUCCESS");r.put("message","成功");return r;}
}
