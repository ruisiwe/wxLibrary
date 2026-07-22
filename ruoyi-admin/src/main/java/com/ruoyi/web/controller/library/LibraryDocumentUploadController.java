package com.ruoyi.web.controller.library;

import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.dto.DocumentUploadCommitRequest;
import com.ruoyi.library.service.DocumentUploadService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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
import org.springframework.web.multipart.MultipartFile;

/** 后台文档预处理、缩略图和确认保存接口。 */
@RestController
@RequestMapping("/library/document-upload")
public class LibraryDocumentUploadController extends BaseController
{
    private final DocumentUploadService uploadService;

    public LibraryDocumentUploadController(DocumentUploadService uploadService)
    {
        this.uploadService = uploadService;
    }

    /** 上传原文件并同步生成试看 PDF 和首页缩略图。 */
    @PreAuthorize("@ss.hasPermi('library:document:add') and @ss.hasPermi('library:document:upload')")
    @Log(title = "文档文件预处理", businessType = BusinessType.IMPORT)
    @PostMapping("/prepare")
    public AjaxResult prepare(@RequestParam("file") MultipartFile file)
    {
        return success(uploadService.prepare(file, getUsername()));
    }

    /** 读取当前管理用户临时会话的 JPG 缩略图。 */
    @PreAuthorize("@ss.hasPermi('library:document:upload')")
    @GetMapping("/session/{sessionId}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable String sessionId)
    {
        Path thumbnail = uploadService.thumbnail(sessionId, getUsername());
        try
        {
            return ResponseEntity.ok()
                    .cacheControl(CacheControl.noStore())
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(Files.readAllBytes(thumbnail));
        }
        catch (IOException exception) { throw new ServiceException("缩略图读取失败，请重新处理"); }
    }

    /** 使用管理员选择的图片替换当前临时缩略图。 */
    @PreAuthorize("@ss.hasPermi('library:document:upload')")
    @Log(title = "替换文档缩略图", businessType = BusinessType.UPDATE)
    @PutMapping("/session/{sessionId}/thumbnail")
    public AjaxResult replaceThumbnail(@PathVariable String sessionId,
            @RequestParam("file") MultipartFile file)
    {
        return success(uploadService.replaceThumbnail(sessionId, file, getUsername()));
    }

    /** 确认元数据后上传三个私有对象并新增文档。 */
    @PreAuthorize("@ss.hasPermi('library:document:add') and @ss.hasPermi('library:document:upload')")
    @Log(title = "新增文档并上传文件", businessType = BusinessType.INSERT)
    @PostMapping("/session/{sessionId}/commit")
    public AjaxResult commit(@PathVariable String sessionId,
            @RequestBody DocumentUploadCommitRequest request)
    {
        return success(uploadService.commit(sessionId, request, getUsername()));
    }

    /** 替换已保存文档的私有缩略图。 */
    @PreAuthorize("@ss.hasPermi('library:document:edit') and @ss.hasPermi('library:document:upload')")
    @Log(title = "替换已保存文档缩略图", businessType = BusinessType.UPDATE)
    @PutMapping("/document/{documentId}/thumbnail")
    public AjaxResult replaceSavedThumbnail(@PathVariable Long documentId,
            @RequestParam("file") MultipartFile file)
    {
        return success(uploadService.replaceSavedThumbnail(documentId, file, getUsername()));
    }

    /** 取消并清理尚未保存的文档上传会话。 */
    @PreAuthorize("@ss.hasPermi('library:document:upload')")
    @Log(title = "取消文档文件预处理", businessType = BusinessType.CLEAN)
    @DeleteMapping("/session/{sessionId}")
    public AjaxResult cancel(@PathVariable String sessionId)
    {
        uploadService.cancel(sessionId, getUsername());
        return success();
    }
}
