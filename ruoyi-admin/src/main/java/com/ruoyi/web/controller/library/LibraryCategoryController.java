package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlCategory;
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
import org.springframework.web.bind.annotation.RestController;

/** 后台文档分类管理接口。 */
@RestController
@RequestMapping("/library/category")
public class LibraryCategoryController extends BaseController
{
    private final DocumentService documentService;

    public LibraryCategoryController(DocumentService documentService) { this.documentService = documentService; }

    /** 分页查询文档分类。 */
    @PreAuthorize("@ss.hasPermi('library:category:list')")
    @GetMapping("/list")
    public TableDataInfo list(WlCategory query)
    {
        startPage();
        List<WlCategory> list = documentService.listCategories(query);
        return getDataTable(list);
    }

    /** 查询后台可选择的分类图标。 */
    @PreAuthorize("@ss.hasPermi('library:category:list')")
    @GetMapping("/icon-options")
    public AjaxResult iconOptions()
    {
        return success(documentService.listCategoryIconOptions());
    }

    /** 查询文档分类详情。 */
    @PreAuthorize("@ss.hasPermi('library:category:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { return success(documentService.getCategory(id)); }

    /** 新增文档分类。 */
    @PreAuthorize("@ss.hasPermi('library:category:add')")
    @Log(title = "文档分类", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WlCategory category)
    {
        return toAjax(documentService.addCategory(category, getUsername()));
    }

    /** 修改文档分类。 */
    @PreAuthorize("@ss.hasPermi('library:category:edit')")
    @Log(title = "文档分类", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WlCategory category)
    {
        return toAjax(documentService.updateCategory(category, getUsername()));
    }

    /** 删除空文档分类。 */
    @PreAuthorize("@ss.hasPermi('library:category:remove')")
    @Log(title = "文档分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(documentService.removeCategories(ids, getUsername()));
    }
}
