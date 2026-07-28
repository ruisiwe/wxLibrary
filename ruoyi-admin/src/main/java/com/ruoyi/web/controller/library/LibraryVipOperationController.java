package com.ruoyi.web.controller.library;

import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlVipEntitlement;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.VipOperationRequest;
import com.ruoyi.library.dto.VipUserOptionView;
import com.ruoyi.library.service.VipBatchOperationService;
import com.ruoyi.library.service.VipEntitlementService;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台会员开通、续期和补偿接口。 */
@RestController
@RequestMapping("/library/vip-operation")
public class LibraryVipOperationController extends BaseController
{
    private final VipEntitlementService entitlementService;
    private final VipBatchOperationService batchService;

    public LibraryVipOperationController(VipEntitlementService entitlementService,
            VipBatchOperationService batchService)
    {
        this.entitlementService = entitlementService;
        this.batchService = batchService;
    }

    /** 分页查询可执行会员操作的启用微信用户。 */
    @PreAuthorize("@ss.hasPermi('library:vip:operation')")
    @GetMapping("/user-options")
    public TableDataInfo userOptions(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize)
    {
        PageHelper.startPage(Math.max(pageNum, 1), Math.min(Math.max(pageSize, 1), 100));
        List<WlWxUser> users = batchService.userOptions(keyword);
        long total = new PageInfo<>(users).getTotal();
        List<VipUserOptionView> options = users.stream()
                .map(VipUserOptionView::from)
                .collect(Collectors.toList());
        return new TableDataInfo(options, total);
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
        return success(batchService.open(request, getUserId()));
    }

    /** 按明确天数补偿会员，不赠送积分。 */
    @PreAuthorize("@ss.hasPermi('library:vip:operation')")
    @Log(title = "会员补偿", businessType = BusinessType.INSERT)
    @PostMapping("/compensate")
    public AjaxResult compensate(@RequestBody VipOperationRequest request)
    {
        return success(batchService.compensate(request, getUserId()));
    }
}
