package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.dto.VipPageConfigView;
import com.ruoyi.library.service.VipPageConfigService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序 VIP 套餐页权益介绍与客服微信接口。 */
@RestController
@RequestMapping("/wx/vip")
public class WxVipPageController
{
    private final VipPageConfigService configService;

    public WxVipPageController(VipPageConfigService configService)
    {
        this.configService = configService;
    }

    /** 查询已启用的 VIP 权益介绍和客服微信展示信息。 */
    @GetMapping("/page-config")
    public WxApiResponse<VipPageConfigView> pageConfig()
    {
        return WxApiResponse.success(configService.getPublicView());
    }
}
