package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.service.VipOrderService;
import com.ruoyi.library.service.VipPlanService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序会员套餐和支付下单接口。 */
@RestController
@RequestMapping("/wx/vip")
public class WxVipController
{
    private final VipPlanService planService; private final VipOrderService orderService;
    public WxVipController(VipPlanService p,VipOrderService o){planService=p;orderService=o;}
    /** 查询当前可购买的会员套餐。 */
    @GetMapping("/plans") public WxApiResponse<List<WlVipPlan>> plans(){WlVipPlan q=new WlVipPlan();q.setStatus("0");return WxApiResponse.success(planService.list(q));}
    /** 创建会员订单并返回微信 JSAPI 调起支付参数。 */
    @PostMapping("/orders/{planId}") public WxApiResponse<Map<String,String>> create(@PathVariable Long planId){return WxApiResponse.success(orderService.createPrepay(WxUserContext.get(),planId));}
}
