package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.library.domain.WlDocumentConversion;
import com.ruoyi.library.service.DocumentConversionService;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/** 后台文档私有文件上传与转换任务接口。 */
@RestController
@RequestMapping("/library/document-file")
public class LibraryDocumentFileController extends BaseController
{
    private final DocumentConversionService conversionService;

    public LibraryDocumentFileController(DocumentConversionService conversionService)
    {
        this.conversionService = conversionService;
    }

    /** 分页查询文档转换任务。 */
    @PreAuthorize("@ss.hasPermi('library:document:convert')")
    @GetMapping("/list")
    public TableDataInfo list(WlDocumentConversion query)
    {
        startPage();
        List<WlDocumentConversion> list = conversionService.listTasks(query);
        return getDataTable(list);
    }

    /** 查询文档转换任务详情。 */
    @PreAuthorize("@ss.hasPermi('library:document:convert')")
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id) { return success(conversionService.getTask(id)); }

    /** 上传管理员文档原文件并创建待转换任务。 */
    @PreAuthorize("@ss.hasPermi('library:document:upload')")
    @Log(title = "文档原文件上传", businessType = BusinessType.IMPORT)
    @PostMapping("/document/{documentId}/upload")
    public AjaxResult upload(@PathVariable Long documentId, @RequestParam("file") MultipartFile file)
    {
        return success(conversionService.uploadOriginal(documentId, file, getUsername()));
    }

    /** 执行待转换任务，转换状态和失败原因会持久化。 */
    @PreAuthorize("@ss.hasPermi('library:document:convert')")
    @Log(title = "执行文档转换", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/execute")
    public AjaxResult execute(@PathVariable Long id) { return success(conversionService.processTask(id)); }

    /** 为失败任务创建新的待转换版本。 */
    @PreAuthorize("@ss.hasPermi('library:document:convert')")
    @Log(title = "重试文档转换", businessType = BusinessType.UPDATE)
    @PostMapping("/{id}/retry")
    public AjaxResult retry(@PathVariable Long id) { return success(conversionService.retry(id, getUsername())); }
}
