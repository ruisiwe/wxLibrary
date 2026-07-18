package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.service.DocumentConversionService;
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

/** 后台文档元数据与上下架管理接口。 */
@RestController
@RequestMapping("/library/document")
public class LibraryDocumentController extends BaseController
{
    private final DocumentService documentService;
    private final DocumentConversionService conversionService;

    public LibraryDocumentController(DocumentService documentService, DocumentConversionService conversionService)
    {
        this.documentService = documentService;
        this.conversionService = conversionService;
    }

    /** 分页查询文档元数据。 */
    @PreAuthorize("@ss.hasPermi('library:document:list')")
    @GetMapping("/list")
    public TableDataInfo list(WlDocument query)
    {
        startPage();
        List<WlDocument> list = documentService.listDocuments(query);
        return getDataTable(list);
    }

    /** 查询文档详情。 */
    @PreAuthorize("@ss.hasPermi('library:document:list')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { return success(documentService.getDocument(id)); }

    /** 新增文档草稿。 */
    @PreAuthorize("@ss.hasPermi('library:document:add')")
    @Log(title = "文档管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@RequestBody WlDocument document)
    {
        return toAjax(documentService.addDocument(document, getUsername()));
    }

    /** 修改未上架的文档。 */
    @PreAuthorize("@ss.hasPermi('library:document:edit')")
    @Log(title = "文档管理", businessType = BusinessType.UPDATE)
    @PutMapping
    public AjaxResult edit(@RequestBody WlDocument document)
    {
        return toAjax(documentService.updateDocument(document, getUsername()));
    }

    /** 删除未上架的文档。 */
    @PreAuthorize("@ss.hasPermi('library:document:remove')")
    @Log(title = "文档管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{ids}")
    public AjaxResult remove(@PathVariable Long[] ids)
    {
        return toAjax(documentService.removeDocuments(ids, getUsername()));
    }

    /** 上架转换成功且分类已启用的文档。 */
    @PreAuthorize("@ss.hasPermi('library:document:publish')")
    @Log(title = "文档上架", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/publish")
    public AjaxResult publish(@PathVariable Long id)
    {
        conversionService.publishDocument(id, getUsername());
        return success();
    }

    /** 下架文档，关联宣传图片会自动停止公开展示。 */
    @PreAuthorize("@ss.hasPermi('library:document:publish')")
    @Log(title = "文档下架", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/unpublish")
    public AjaxResult unpublish(@PathVariable Long id)
    {
        documentService.unpublishDocument(id, getUsername());
        return success();
    }
}
