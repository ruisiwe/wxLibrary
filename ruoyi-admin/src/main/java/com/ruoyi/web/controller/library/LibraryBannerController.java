package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlBanner;
import com.ruoyi.library.service.BannerManagementService;
import com.ruoyi.library.service.DocumentService;
import java.util.List;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 后台首页宣传图片管理接口。 */
@RestController
@RequestMapping("/library/banner")
public class LibraryBannerController extends BaseController
{
    private final DocumentService documentService;
    private final BannerManagementService bannerManagementService;

    public LibraryBannerController(DocumentService documentService,
            BannerManagementService bannerManagementService)
    {
        this.documentService = documentService;
        this.bannerManagementService = bannerManagementService;
    }

    /** 分页查询首页宣传图片。 */
    @PreAuthorize("@ss.hasPermi('library:banner:list')")
    @GetMapping("/list")
    public TableDataInfo list(WlBanner query)
    {
        startPage();
        List<WlBanner> list = documentService.listBanners(query);
        return getDataTable(list);
    }

    /** 搜索宣传图片关联文档，并标明当前是否可关联。 */
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

    /** 获取首页轮播图私有图片的短时预览地址。 */
    @PreAuthorize("@ss.hasAnyPermi('library:banner:list,library:banner:edit')")
    @GetMapping("/{id}/image")
    public AjaxResult image(@PathVariable Long id)
    {
        return success(bannerManagementService.preview(id));
    }

    /** 新增首页轮播图，并上传裁剪后的本地图片。 */
    @PreAuthorize("@ss.hasPermi('library:banner:add')")
    @Log(title = "首页宣传图片", businessType = BusinessType.INSERT)
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult add(@RequestPart("banner") WlBanner banner,
            @RequestPart("image") MultipartFile image)
    {
        return toAjax(bannerManagementService.add(banner, image, getUsername()));
    }

    /** 修改首页轮播图；未上传新图片时保留原图。 */
    @PreAuthorize("@ss.hasPermi('library:banner:edit')")
    @Log(title = "首页宣传图片", businessType = BusinessType.UPDATE)
    @PutMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AjaxResult edit(@RequestPart("banner") WlBanner banner,
            @RequestPart(value = "image", required = false) MultipartFile image)
    {
        return toAjax(bannerManagementService.update(banner, image, getUsername()));
    }

    /** 删除首页宣传图片。 */
    @PreAuthorize("@ss.hasPermi('library:banner:remove')")
    @Log(title = "首页宣传图片", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(bannerManagementService.remove(ids, getUsername()));
    }
}
