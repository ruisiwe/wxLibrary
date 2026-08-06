package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlVipPageConfig;
import com.ruoyi.library.service.VipPageConfigService;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 后台 VIP 套餐页客服微信配置接口。 */
@RestController
@RequestMapping("/library/vip-page-config")
public class LibraryVipPageConfigController extends BaseController
{
    private final VipPageConfigService configService;

    public LibraryVipPageConfigController(VipPageConfigService configService)
    {
        this.configService = configService;
    }

    /** 查询 VIP 套餐页客服微信配置。 */
    @PreAuthorize("@ss.hasPermi('library:vip:page-config:query')")
    @GetMapping
    public AjaxResult detail()
    {
        return success(configService.getManagementView());
    }

    /** 修改客服提示语，并可同时替换客服微信图片。 */
    @PreAuthorize("@ss.hasPermi('library:vip:page-config:edit')")
    @Log(title = "VIP 权益介绍", businessType = BusinessType.UPDATE)
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult edit(@RequestPart("config") WlVipPageConfig config,
            @RequestPart(value = "image", required = false) MultipartFile image)
    {
        return toAjax(configService.update(config, image, getUsername()));
    }

    /** 清空客服微信图片，保留客服提示语。 */
    @PreAuthorize("@ss.hasPermi('library:vip:page-config:edit')")
    @Log(title = "VIP 权益介绍", businessType = BusinessType.UPDATE)
    @DeleteMapping("/image")
    public AjaxResult clearImage()
    {
        return toAjax(configService.clearImage(getUsername()));
    }
}
