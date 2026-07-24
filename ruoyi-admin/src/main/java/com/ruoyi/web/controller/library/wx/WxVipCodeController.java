package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.domain.WlVipEntitlement;
import com.ruoyi.library.dto.VipCodeRedeemRequest;
import com.ruoyi.library.service.VipCodeService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序会员码兑换接口。 */
@RestController
@RequestMapping("/wx/vip/code")
public class WxVipCodeController
{
    private final VipCodeService service;

    public WxVipCodeController(VipCodeService service) { this.service = service; }

    /** 使用会员码开通或续期当前微信用户会员。 */
    @PostMapping("/redeem")
    public WxApiResponse<WlVipEntitlement> redeem(@RequestBody VipCodeRedeemRequest request)
    {
        return WxApiResponse.success(service.redeem(WxUserContext.get(),
                request == null ? null : request.getCode()));
    }
}
