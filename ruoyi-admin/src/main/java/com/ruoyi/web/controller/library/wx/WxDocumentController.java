package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.dto.DocumentSummaryDto;
import com.ruoyi.library.dto.DocumentUnlockRequest;
import com.ruoyi.library.dto.DocumentUnlockResult;
import com.ruoyi.library.dto.FileAuthorization;
import com.ruoyi.library.dto.FileDisclaimerDto;
import com.ruoyi.library.dto.OriginalFileRequest;
import com.ruoyi.library.service.DocumentAccessService;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import javax.servlet.http.HttpServletRequest;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序文档兑换、收藏和私有文件授权接口。 */
@RestController
@RequestMapping("/wx/documents")
public class WxDocumentController
{
    private final DocumentAccessService documentAccessService;

    public WxDocumentController(DocumentAccessService documentAccessService)
    {
        this.documentAccessService = documentAccessService;
    }

    /** 使用文档价格快照兑换永久阅读权限。 */
    @PostMapping("/{id}/unlock")
    public WxApiResponse<DocumentUnlockResult> unlock(@PathVariable Long id,
            @RequestBody DocumentUnlockRequest request)
    {
        return WxApiResponse.success(documentAccessService.unlock(
                WxUserContext.get(), id, request == null ? null : request.getRequestId(),
                request != null && request.isFreeOnly()));
    }

    /** 获取试读 PDF 的短时访问地址。 */
    @GetMapping("/{id}/preview")
    public WxApiResponse<FileAuthorization> preview(@PathVariable Long id, HttpServletRequest request)
    {
        return WxApiResponse.success(documentAccessService.authorizePreview(
                WxUserContext.get(), id, request.getRemoteAddr()));
    }

    /** 查询当前文件发送免责声明及本版本免提示状态。 */
    @GetMapping("/file-disclaimer")
    public WxApiResponse<FileDisclaimerDto> fileDisclaimer()
    {
        return WxApiResponse.success(documentAccessService.fileDisclaimer(WxUserContext.get()));
    }

    /** 确认当前免责声明后获取原文件短时地址，接收人由微信面板选择。 */
    @PostMapping("/{id}/original")
    public WxApiResponse<FileAuthorization> original(@PathVariable Long id,
            @RequestBody OriginalFileRequest originalRequest, HttpServletRequest request)
    {
        return WxApiResponse.success(documentAccessService.authorizeOriginalFile(
                WxUserContext.get(), id, originalRequest, request.getRemoteAddr()));
    }

    /** 收藏已上架文档。 */
    @PostMapping("/{id}/favorite")
    public WxApiResponse<Map<String, Boolean>> favorite(@PathVariable Long id)
    {
        return WxApiResponse.success(Collections.singletonMap("favorite",
                documentAccessService.favorite(WxUserContext.get(), id)));
    }

    /** 取消收藏文档。 */
    @DeleteMapping("/{id}/favorite")
    public WxApiResponse<Map<String, Boolean>> unfavorite(@PathVariable Long id)
    {
        return WxApiResponse.success(Collections.singletonMap("favorite",
                documentAccessService.unfavorite(WxUserContext.get(), id)));
    }

    /** 查询当前用户已兑换且仍可用的文档。 */
    @GetMapping("/unlocked")
    public WxApiResponse<List<DocumentSummaryDto>> unlocked()
    {
        return WxApiResponse.success(documentAccessService.listUnlocked(WxUserContext.get()));
    }

    /** 查询当前用户收藏且仍可用的文档。 */
    @GetMapping("/favorites")
    public WxApiResponse<List<DocumentSummaryDto>> favorites()
    {
        return WxApiResponse.success(documentAccessService.listFavorites(WxUserContext.get()));
    }
}
