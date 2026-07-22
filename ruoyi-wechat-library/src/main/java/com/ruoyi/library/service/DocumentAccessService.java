package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.domain.WlDocumentUnlock;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.DocumentSummaryDto;
import com.ruoyi.library.dto.DocumentUnlockResult;
import com.ruoyi.library.dto.FileAuthorization;
import com.ruoyi.library.dto.FileDisclaimerDto;
import com.ruoyi.library.dto.OriginalFileRequest;
import com.ruoyi.library.agreement.WxAgreementService;
import com.ruoyi.library.mapper.WlDocumentMapper;
import com.ruoyi.library.mapper.WlDocumentUnlockMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.storage.PrivateFileUrlSigner;
import java.net.URL;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 文档兑换、收藏和私有文件访问授权服务。 */
@Service
public class DocumentAccessService
{
    private static final Duration FILE_URL_TTL = Duration.ofMinutes(5);

    private final PointService pointService;
    private final WlWxUserMapper userMapper;
    private final WlDocumentMapper documentMapper;
    private final WlDocumentUnlockMapper unlockMapper;
    private final ObjectProvider<PrivateFileUrlSigner> signerProvider;
    private final WxAgreementService agreementService;

    public DocumentAccessService(PointService pointService, WlWxUserMapper userMapper,
            WlDocumentMapper documentMapper, WlDocumentUnlockMapper unlockMapper,
            ObjectProvider<PrivateFileUrlSigner> signerProvider, WxAgreementService agreementService)
    {
        this.pointService = pointService;
        this.userMapper = userMapper;
        this.documentMapper = documentMapper;
        this.unlockMapper = unlockMapper;
        this.signerProvider = signerProvider;
        this.agreementService = agreementService;
    }

    @Transactional
    public DocumentUnlockResult unlock(Long userId, Long documentId, String requestId)
    {
        validateRequestId(requestId);
        WlWxUser currentUser = requireEnabledUser(userMapper.selectById(userId));
        WlDocumentUnlock existing = unlockMapper.selectUnlock(userId, documentId);
        if (existing != null) return existingResult(existing, currentUser);
        WlDocument document = requirePublishedDocument(documentId);
        WlWxUser lockedUser = requireEnabledUser(userMapper.selectByIdForUpdate(userId));
        existing = unlockMapper.selectUnlock(userId, documentId);
        if (existing != null) return existingResult(existing, lockedUser);
        long price = document.getPointPrice() == null ? 0L : document.getPointPrice();
        WlPointRecord record = pointService.deductAfterLock(lockedUser, price,
                "DOCUMENT_UNLOCK", pointBizNo(documentId, requestId),
                "兑换文档：" + document.getTitle() + "（价格快照：" + price + "积分）");
        WlDocumentUnlock unlock = new WlDocumentUnlock();
        unlock.setUserId(userId);
        unlock.setDocumentId(documentId);
        unlock.setSpentPoints(price);
        unlock.setPointRecordId(record.getId());
        unlock.setUnlockTime(new Date());
        unlock.setCreateBy("wx:" + userId);
        if (unlockMapper.insertUnlock(unlock) != 1) throw new ServiceException("文档兑换失败，请重试");
        return new DocumentUnlockResult(documentId, true, price, record.getAfterBalance());
    }

    public FileAuthorization authorizePreview(Long userId, Long documentId, String clientIp)
    {
        requireEnabledUser(userMapper.selectById(userId));
        WlDocument document = requirePublishedDocument(documentId);
        requirePreviewBoundary(document);
        requireObjectKey(document.getPreviewObjectKey(), "文档暂未生成试读文件");
        FileAuthorization authorization = sign(document.getPreviewObjectKey(), null);
        recordView(userId, documentId, "PREVIEW", clientIp, true);
        return authorization;
    }

    public FileDisclaimerDto fileDisclaimer(Long userId)
    {
        requireEnabledUser(userMapper.selectById(userId));
        return agreementService.fileDisclaimer(userId);
    }

    public FileAuthorization authorizeOriginalFile(Long userId, Long documentId,
            OriginalFileRequest request, String clientIp)
    {
        requireEnabledUser(userMapper.selectById(userId));
        WlDocument document = requirePublishedDocument(documentId);
        requireUnlock(userId, documentId);
        agreementService.validateFileDisclaimer(userId, request, clientIp);
        requireObjectKey(document.getOriginalObjectKey(), "文档原文件暂不可用");
        String fileName = safeFileName(document.getTitle(), document.getFileFormat());
        FileAuthorization authorization = sign(document.getOriginalObjectKey(), fileName);
        recordView(userId, documentId, "ORIGINAL", clientIp, false);
        return authorization;
    }

    public boolean favorite(Long userId, Long documentId)
    {
        requireEnabledUser(userMapper.selectById(userId));
        requirePublishedDocument(documentId);
        unlockMapper.saveFavorite(userId, documentId);
        return true;
    }

    public boolean unfavorite(Long userId, Long documentId)
    {
        requireEnabledUser(userMapper.selectById(userId));
        unlockMapper.deleteFavorite(userId, documentId);
        return false;
    }

    public List<DocumentSummaryDto> listUnlocked(Long userId)
    {
        requireEnabledUser(userMapper.selectById(userId));
        return unlockMapper.selectUnlockedDocuments(userId);
    }

    public List<DocumentSummaryDto> listFavorites(Long userId)
    {
        requireEnabledUser(userMapper.selectById(userId));
        return unlockMapper.selectFavoriteDocuments(userId);
    }

    public boolean isUnlocked(Long userId, Long documentId)
    {
        requireEnabledUser(userMapper.selectById(userId));
        return unlockMapper.selectUnlock(userId, documentId) != null;
    }

    public boolean isFavorite(Long userId, Long documentId)
    {
        requireEnabledUser(userMapper.selectById(userId));
        return unlockMapper.countFavorite(userId, documentId) > 0;
    }

    private DocumentUnlockResult existingResult(WlDocumentUnlock existing, WlWxUser user)
    {
        return new DocumentUnlockResult(existing.getDocumentId(), true,
                existing.getSpentPoints(), user.getPointBalance());
    }

    private WlDocument requirePublishedDocument(Long documentId)
    {
        if (documentId == null || documentId <= 0) throw new ServiceException("文档编号不正确");
        WlDocument document = documentMapper.selectDocumentById(documentId);
        if (document == null || !"PUBLISHED".equals(document.getPublishStatus()))
            throw new ServiceException("文档不存在或已下架");
        if (!"SUCCESS".equals(document.getConversionStatus()))
            throw new ServiceException("文档文件尚未准备完成");
        return document;
    }

    private void requireUnlock(Long userId, Long documentId)
    {
        if (unlockMapper.selectUnlock(userId, documentId) == null)
            throw new ServiceException("请先兑换文档");
    }

    private WlWxUser requireEnabledUser(WlWxUser user)
    {
        if (user == null) throw new ServiceException("微信用户不存在");
        if (!"0".equals(user.getStatus())) throw new ServiceException("当前账号已停用，请联系管理员");
        return user;
    }

    private FileAuthorization sign(String objectKey, String fileName)
    {
        PrivateFileUrlSigner signer = signerProvider.getIfAvailable();
        if (signer == null) throw new ServiceException("文件服务暂不可用，请稍后重试");
        URL url = signer.signGetUrl(objectKey, FILE_URL_TTL, fileName);
        if (url == null) throw new ServiceException("文件授权失败，请稍后重试");
        return new FileAuthorization(fileName, url.toString(), FILE_URL_TTL.getSeconds());
    }

    private void recordView(Long userId, Long documentId, String type, String clientIp, boolean increment)
    {
        unlockMapper.insertView(userId, documentId, type, normalizeIp(clientIp));
        if (increment) unlockMapper.incrementViewCount(documentId);
    }

    private String normalizeIp(String clientIp)
    {
        if (clientIp == null) return null;
        String value = clientIp.trim();
        return value.length() <= 64 ? value : value.substring(0, 64);
    }

    private String safeFileName(String title, String format)
    {
        String safeTitle = title == null ? "文档" : title.replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_").trim();
        if (safeTitle.isEmpty()) safeTitle = "文档";
        if (safeTitle.length() > 120) safeTitle = safeTitle.substring(0, 120);
        String extension = format == null ? "bin" : format.toLowerCase(java.util.Locale.ROOT);
        return safeTitle + "." + extension;
    }

    private void validateRequestId(String requestId)
    {
        if (requestId == null || requestId.trim().isEmpty()) throw new ServiceException("请求编号不能为空");
        if (requestId.trim().length() > 80) throw new ServiceException("请求编号不能超过80个字符");
    }

    private String pointBizNo(Long documentId, String requestId)
    {
        return "DOCUMENT_UNLOCK:" + documentId + ":" + requestId.trim();
    }

    private void requireObjectKey(String value, String message)
    {
        if (value == null || value.trim().isEmpty()) throw new ServiceException(message);
    }

    private void requirePreviewBoundary(WlDocument document)
    {
        Integer pageCount = document.getPageCount();
        Integer previewPages = document.getPreviewPages();
        if (pageCount == null || previewPages == null || previewPages <= 0 || previewPages >= pageCount)
            throw new ServiceException("文档试读页数配置不正确");
    }
}
