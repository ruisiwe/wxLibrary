package com.ruoyi.web.controller.library.wx;

import java.util.List;
import javax.servlet.http.HttpServletRequest;
import com.ruoyi.library.agreement.WxAgreementService;
import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.domain.WlAgreement;
import com.ruoyi.library.dto.AgreementAcceptRequest;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序协议查询与确认接口。 */
@RestController
@RequestMapping("/wx")
public class WxAgreementController
{
    private final WxAgreementService agreementService;

    public WxAgreementController(WxAgreementService agreementService) { this.agreementService = agreementService; }

    /** 匿名查询当前生效的用户隐私协议。 */
    @GetMapping("/public/agreements/current")
    public WxApiResponse<List<WlAgreement>> current()
    {
        return WxApiResponse.success(agreementService.current());
    }

    /** 当前登录用户确认最新协议。 */
    @PostMapping("/agreements/accept")
    public WxApiResponse<Void> accept(@RequestBody AgreementAcceptRequest request, HttpServletRequest servletRequest)
    {
        agreementService.validateCurrentAcceptance(request.isPrivacyAccepted(), request.getPrivacyVersion());
        agreementService.acceptCurrent(WxUserContext.get(), request.getPrivacyVersion(), servletRequest.getRemoteAddr());
        return WxApiResponse.success(null);
    }
}
