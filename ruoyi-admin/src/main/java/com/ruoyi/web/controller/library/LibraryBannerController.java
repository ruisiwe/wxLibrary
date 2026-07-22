package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlBanner;
import com.ruoyi.library.service.DocumentService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** 后台首页宣传图片管理接口。 */
@RestController
@RequestMapping("/library/banner")
public class LibraryBannerController extends BaseController
{
    private final DocumentService documentService;

    public LibraryBannerController(DocumentService documentService) { this.documentService = documentService; }

    /** 分页查询首页宣传图片。 */
    @PreAuthorize("@ss.hasPermi('library:banner:list')")
    @GetMapping("/list")
    public TableDataInfo list(WlBanner query)
    {
        startPage();
        List<WlBanner> list = documentService.listBanners(query);
        return getDataTable(list);
    }

    /** 搜索宣传图片可关联的已发布文档。 */
    @PreAuthorize("@ss.hasAnyPermi('library:banner:add,library:banner:edit')")
    @GetMapping("/document-options")
    public AjaxResult documentOptions(@RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "20") int pageSize)
    {
        return success(documentService.listBannerDocumentOptions(keyword, pageNum, pageSize));
    }

    /** 查询首页宣传图片详情。 */
    @PreAuthorize("@ss.hasPermi('library:banner:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { return success(documentService.getBanner(id)); }

    /** 新增首页宣传图片，只能关联已上架文档。 */
    @PreAuthorize("@ss.hasPermi('library:banner:add')")
    @Log(title = "首页宣传图片", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WlBanner banner)
    {
        return toAjax(documentService.addBanner(banner, getUsername()));
    }

    /** 修改首页宣传图片。 */
    @PreAuthorize("@ss.hasPermi('library:banner:edit')")
    @Log(title = "首页宣传图片", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WlBanner banner)
    {
        return toAjax(documentService.updateBanner(banner, getUsername()));
    }

    /** 删除首页宣传图片。 */
    @PreAuthorize("@ss.hasPermi('library:banner:remove')")
    @Log(title = "首页宣传图片", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(documentService.removeBanners(ids, getUsername()));
    }
}
