package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlVipBenefit;
import com.ruoyi.library.service.VipBenefitService;
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

/** 后台 VIP 权益介绍管理接口。 */
@RestController
@RequestMapping("/library/vip-benefit")
public class LibraryVipBenefitController extends BaseController
{
    private final VipBenefitService benefitService;

    public LibraryVipBenefitController(VipBenefitService benefitService)
    {
        this.benefitService = benefitService;
    }

    /** 分页查询 VIP 权益介绍。 */
    @PreAuthorize("@ss.hasPermi('library:vip:benefit:list')")
    @GetMapping("/list")
    public TableDataInfo list(WlVipBenefit query)
    {
        startPage();
        List<WlVipBenefit> list = benefitService.list(query);
        return getDataTable(list);
    }

    /** 查询单条 VIP 权益介绍。 */
    @PreAuthorize("@ss.hasPermi('library:vip:benefit:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return success(benefitService.get(id));
    }

    /** 新增 VIP 权益介绍。 */
    @PreAuthorize("@ss.hasPermi('library:vip:benefit:add')")
    @Log(title = "VIP 权益介绍", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WlVipBenefit benefit)
    {
        return toAjax(benefitService.add(benefit, getUsername()));
    }

    /** 修改 VIP 权益介绍。 */
    @PreAuthorize("@ss.hasPermi('library:vip:benefit:edit')")
    @Log(title = "VIP 权益介绍", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WlVipBenefit benefit)
    {
        return toAjax(benefitService.edit(benefit, getUsername()));
    }

    /** 删除 VIP 权益介绍。 */
    @PreAuthorize("@ss.hasPermi('library:vip:benefit:remove')")
    @Log(title = "VIP 权益介绍", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(benefitService.remove(id, getUsername()));
    }
}
