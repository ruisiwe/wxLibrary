package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlVipRefund;
import com.ruoyi.library.dto.VipRefundRequest;
import com.ruoyi.library.service.VipRefundService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台会员全额退款管理接口。 */
@RestController
@RequestMapping("/library/vip-refund")
public class LibraryVipRefundController extends BaseController
{
    private final VipRefundService service;

    public LibraryVipRefundController(VipRefundService service) { this.service = service; }

    /** 分页查询会员退款。 */
    @PreAuthorize("@ss.hasPermi('library:vip:refund')")
    @GetMapping("/list")
    public TableDataInfo list(WlVipRefund query)
    {
        startPage();
        List<WlVipRefund> rows = service.list(query);
        return getDataTable(rows);
    }

    /** 发起全额退款，二次确认令牌禁止进入请求和响应操作日志。 */
    @PreAuthorize("@ss.hasPermi('library:vip:refund')")
    @Log(title = "会员全额退款", businessType = BusinessType.UPDATE,
            excludeParamNames = {"confirmationToken"}, isSaveResponseData = false)
    @PostMapping
    public AjaxResult refund(@RequestBody VipRefundRequest request)
    {
        return success(service.requestFullRefund(request == null ? null : request.getOrderId(),
                request == null ? null : request.getReason(),
                request == null ? null : request.getConfirmationToken(), getUserId()));
    }
}
