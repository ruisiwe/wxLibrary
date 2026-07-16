package com.ruoyi.web.controller.library;

import java.util.List;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.library.agreement.WxAgreementService;
import com.ruoyi.library.domain.WlAgreement;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台协议版本管理接口。 */
@RestController
@RequestMapping("/library/agreement")
public class LibraryAgreementController extends BaseController
{
    private final WxAgreementService agreementService;

    public LibraryAgreementController(WxAgreementService agreementService)
    {
        this.agreementService = agreementService;
    }

    /** 分页查询协议版本。 */
    @PreAuthorize("@ss.hasPermi('library:agreement:list')")
    @GetMapping("/list")
    public TableDataInfo list(WlAgreement query)
    {
        startPage();
        List<WlAgreement> list = agreementService.list(query);
        return getDataTable(list);
    }

    /** 查询协议详情。 */
    @PreAuthorize("@ss.hasPermi('library:agreement:query')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { return success(agreementService.detail(id)); }

    /** 新增协议草稿。 */
    @PreAuthorize("@ss.hasPermi('library:agreement:add')")
    @PostMapping
    public AjaxResult add(@RequestBody WlAgreement agreement)
    {
        return toAjax(agreementService.addDraft(agreement, getUsername()));
    }

    /** 修改协议草稿。 */
    @PreAuthorize("@ss.hasPermi('library:agreement:edit')")
    @PutMapping
    public AjaxResult edit(@RequestBody WlAgreement agreement)
    {
        agreement.setUpdateBy(getUsername());
        return toAjax(agreementService.updateDraft(agreement));
    }

    /** 发布协议新版本并停用同类型旧版本。 */
    @PreAuthorize("@ss.hasPermi('library:agreement:publish')")
    @PutMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id)
    {
        agreementService.publish(id, getUsername());
        return success();
    }
}
