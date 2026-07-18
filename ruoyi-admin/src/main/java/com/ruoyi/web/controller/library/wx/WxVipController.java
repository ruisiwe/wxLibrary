package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.dto.VipOrderStatusView;
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
    private final VipPlanService planService;
    private final VipOrderService orderService;

    public WxVipController(VipPlanService planService, VipOrderService orderService)
    {
        this.planService = planService;
        this.orderService = orderService;
    }

    /** 查询当前可购买的会员套餐。 */
    @GetMapping("/plans")
    public WxApiResponse<List<WlVipPlan>> plans()
    {
        WlVipPlan query = new WlVipPlan();
        query.setStatus("0");
        return WxApiResponse.success(planService.list(query));
    }

    /** 创建会员订单并返回微信 JSAPI 调起支付参数及商户订单号。 */
    @PostMapping("/orders/{planId}")
    public WxApiResponse<Map<String, String>> create(@PathVariable Long planId)
    {
        return WxApiResponse.success(orderService.createPrepay(WxUserContext.get(), planId));
    }

    /** 查询当前微信用户自己的会员订单状态，支付结果以该接口为准。 */
    @GetMapping("/orders/status/{merchantOrderNo}")
    public WxApiResponse<VipOrderStatusView> status(@PathVariable String merchantOrderNo)
    {
        return WxApiResponse.success(new VipOrderStatusView(
                orderService.getForUser(WxUserContext.get(), merchantOrderNo)));
    }
}
