package com.ruoyi.web.controller.library;
import com.ruoyi.common.core.controller.BaseController;import com.ruoyi.common.core.domain.AjaxResult;import com.ruoyi.common.core.page.TableDataInfo;import com.ruoyi.library.domain.WlVipOrder;import com.ruoyi.library.service.VipOrderService;import java.util.List;import org.springframework.security.access.prepost.PreAuthorize;import org.springframework.web.bind.annotation.*;
/** 后台会员支付订单查询接口。 */
@RestController @RequestMapping("/library/vip-order")
public class LibraryVipOrderController extends BaseController
{
 private final VipOrderService service; public LibraryVipOrderController(VipOrderService s){service=s;}
 /** 分页查询会员支付订单。 */ @PreAuthorize("@ss.hasPermi('library:vip:order')") @GetMapping("/list") public TableDataInfo list(WlVipOrder q){startPage();List<WlVipOrder> rows=service.list(q);return getDataTable(rows);}
 /** 查询会员支付订单详情。 */ @PreAuthorize("@ss.hasPermi('library:vip:order')") @GetMapping("/{id}") public AjaxResult detail(@PathVariable Long id){return success(service.get(id));}
}
