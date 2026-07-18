package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlPointRule;
import com.ruoyi.library.service.PointService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台积分规则管理接口。 */
@RestController
@RequestMapping("/library/point-rule")
public class LibraryPointRuleController extends BaseController
{
    private final PointService pointService;

    public LibraryPointRuleController(PointService pointService) { this.pointService = pointService; }

    /** 分页查询积分规则。 */
    @PreAuthorize("@ss.hasPermi('library:points:rule')")
    @GetMapping("/list")
    public TableDataInfo list(WlPointRule query)
    {
        startPage();
        List<WlPointRule> list = pointService.listRules(query);
        return getDataTable(list);
    }

    /** 查询积分规则详情。 */
    @PreAuthorize("@ss.hasPermi('library:points:rule')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { return success(pointService.getRule(id)); }

    /** 修改积分规则，新配置只影响修改后的任务奖励。 */
    @PreAuthorize("@ss.hasPermi('library:points:rule')")
    @Log(title = "积分规则管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WlPointRule rule)
    {
        return toAjax(pointService.updateRule(rule, getUsername()));
    }
}
