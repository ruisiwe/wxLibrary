package com.ruoyi.web.controller.library;
import com.ruoyi.common.annotation.Log;import com.ruoyi.common.core.controller.BaseController;import com.ruoyi.common.core.domain.AjaxResult;import com.ruoyi.common.core.page.TableDataInfo;import com.ruoyi.common.enums.BusinessType;import com.ruoyi.library.domain.WlVipRefund;import com.ruoyi.library.dto.VipRefundRequest;import com.ruoyi.library.service.VipRefundService;import java.util.List;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.web.bind.annotation.*;
/** 后台会员全额退款管理接口。 */
@RestController @RequestMapping("/library/vip-refund")
public class LibraryVipRefundController extends BaseController
{
 private final VipRefundService service;public LibraryVipRefundController(VipRefundService s){service=s;}
 /** 分页查询会员退款。 */ @PreAuthorize("@ss.hasPermi('library:vip:refund')") @GetMapping("/list") public TableDataInfo list(WlVipRefund q){startPage();List<WlVipRefund> rows=service.list(q);return getDataTable(rows);}
 /** 对已支付订单发起全额退款，必须提供原因和二次确认令牌。 */ @PreAuthorize("@ss.hasPermi('library:vip:refund')") @Log(title="会员全额退款",businessType=BusinessType.UPDATE) @PostMapping public AjaxResult refund(@RequestBody VipRefundRequest r){return success(service.requestFullRefund(r==null?null:r.getOrderId(),r==null?null:r.getReason(),r==null?null:r.getConfirmationToken(),getUserId()));}
}
