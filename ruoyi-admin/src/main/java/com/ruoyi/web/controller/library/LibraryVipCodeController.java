package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipCode;
import com.ruoyi.library.dto.VipCodeGenerateRequest;
import com.ruoyi.library.service.VipCodeService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 后台会员码批量生成、查询和禁用接口。 */
@RestController
@RequestMapping("/library/vip-code")
public class LibraryVipCodeController extends BaseController
{
    private final VipCodeService service;

    public LibraryVipCodeController(VipCodeService service) { this.service = service; }

    /** 分页查询会员码掩码和使用状态。 */
    @PreAuthorize("@ss.hasPermi('library:vip:code')")
    @GetMapping("/list")
    public TableDataInfo list(WlVipCode query)
    {
        startPage();
        return getDataTable(service.list(query));
    }

    /** 批量生成高熵会员码，明文只在本次响应中返回且禁止写入操作日志。 */
    @PreAuthorize("@ss.hasPermi('library:vip:code')")
    @Log(title = "会员码生成", businessType = BusinessType.INSERT,
            isSaveResponseData = false)
    @PostMapping("/generate")
    public AjaxResult generate(@RequestBody VipCodeGenerateRequest request)
    {
        if (request == null || request.getCount() == null)
            throw new ServiceException("生成数量不能为空");
        return success(service.generate(request.getPlanId(), request.getCount(),
                request.getExpiresTime(), getUsername()));
    }

    /** 禁用未使用的会员码。 */
    @PreAuthorize("@ss.hasPermi('library:vip:code')")
    @Log(title = "会员码禁用", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/disable")
    public AjaxResult disable(@PathVariable Long id)
    {
        return toAjax(service.disable(id, getUsername()));
    }
}
