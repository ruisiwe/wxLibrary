package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.service.VipPlanService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台会员套餐管理接口。 */
@RestController
@RequestMapping("/library/vip-plan")
public class LibraryVipPlanController extends BaseController
{
    private final VipPlanService planService;

    public LibraryVipPlanController(VipPlanService planService) { this.planService = planService; }

    /** 分页查询会员套餐。 */
    @PreAuthorize("@ss.hasPermi('library:vip:plan')")
    @GetMapping("/list")
    public TableDataInfo list(WlVipPlan query)
    {
        startPage();
        List<WlVipPlan> list = planService.list(query);
        return getDataTable(list);
    }

    /** 查询会员套餐详情。 */
    @PreAuthorize("@ss.hasPermi('library:vip:plan')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { return success(planService.get(id)); }

    /** 新增会员套餐。 */
    @PreAuthorize("@ss.hasPermi('library:vip:plan')")
    @Log(title = "会员套餐管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WlVipPlan plan) { return toAjax(planService.add(plan, getUsername())); }

    /** 修改会员套餐，新配置仅影响后续开通和续期。 */
    @PreAuthorize("@ss.hasPermi('library:vip:plan')")
    @Log(title = "会员套餐管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WlVipPlan plan) { return toAjax(planService.edit(plan, getUsername())); }

    /** 删除会员套餐。 */
    @PreAuthorize("@ss.hasPermi('library:vip:plan')")
    @Log(title = "会员套餐管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) { return toAjax(planService.remove(id, getUsername())); }
}
