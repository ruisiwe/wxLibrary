package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipEntitlement;
import com.ruoyi.library.dto.VipOperationRequest;
import com.ruoyi.library.service.VipEntitlementService;
import com.ruoyi.library.service.VipPlanService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台会员开通、续期和补偿接口。 */
@RestController
@RequestMapping("/library/vip-operation")
public class LibraryVipOperationController extends BaseController
{
    private final VipPlanService planService;
    private final VipEntitlementService entitlementService;

    public LibraryVipOperationController(VipPlanService planService, VipEntitlementService entitlementService)
    {
        this.planService = planService;
        this.entitlementService = entitlementService;
    }

    /** 分页查询会员权益台账。 */
    @PreAuthorize("@ss.hasPermi('library:vip:operation')")
    @GetMapping("/list")
    public TableDataInfo list(WlVipEntitlement query)
    {
        startPage();
        List<WlVipEntitlement> list = entitlementService.list(query);
        return getDataTable(list);
    }

    /** 使用当前套餐快照人工开通或续期会员。 */
    @PreAuthorize("@ss.hasPermi('library:vip:operation')")
    @Log(title = "会员人工开通续期", businessType = BusinessType.INSERT)
    @PostMapping("/open")
    public AjaxResult open(@RequestBody VipOperationRequest request)
    {
        requireRequest(request);
        if (request.getPlanId() == null) throw new ServiceException("会员套餐编号不能为空");
        return success(entitlementService.openOrRenew(request.getUserId(), planService.getEnabled(request.getPlanId()),
                "MANUAL", request.getBizNo(), getUserId(), request.getReason()));
    }

    /** 按明确天数补偿会员，不赠送积分。 */
    @PreAuthorize("@ss.hasPermi('library:vip:operation')")
    @Log(title = "会员补偿", businessType = BusinessType.INSERT)
    @PostMapping("/compensate")
    public AjaxResult compensate(@RequestBody VipOperationRequest request)
    {
        requireRequest(request);
        if (request.getDays() == null) throw new ServiceException("补偿天数不能为空");
        return success(entitlementService.compensate(request.getUserId(), request.getDays(), getUserId(),
                request.getReason(), request.getBizNo()));
    }

    private void requireRequest(VipOperationRequest request)
    {
        if (request == null) throw new ServiceException("会员操作请求不能为空");
        if (request.getUserId() == null) throw new ServiceException("微信用户编号不能为空");
        if (request.getBizNo() == null || request.getBizNo().trim().isEmpty())
            throw new ServiceException("会员业务编号不能为空");
        if (request.getReason() == null || request.getReason().trim().isEmpty())
            throw new ServiceException("操作原因不能为空");
    }
}
