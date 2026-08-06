package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.domain.WlDocumentUnlock;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.DocumentUnlockResult;
import com.ruoyi.library.dto.DocumentSummaryDto;
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
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentAccessServiceTest
{
    private PointService pointService;
    private WlWxUserMapper userMapper;
    private WlDocumentMapper documentMapper;
    private WlDocumentUnlockMapper unlockMapper;
    private PrivateFileUrlSigner urlSigner;
    private WxAgreementService agreementService;
    private DocumentCoverUrlService coverUrlService;
    private DocumentAccessService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp() throws Exception
    {
        pointService = mock(PointService.class);
        userMapper = mock(WlWxUserMapper.class);
        documentMapper = mock(WlDocumentMapper.class);
        unlockMapper = mock(WlDocumentUnlockMapper.class);
        urlSigner = mock(PrivateFileUrlSigner.class);
        agreementService = mock(WxAgreementService.class);
        coverUrlService = mock(DocumentCoverUrlService.class);
        ObjectProvider<PrivateFileUrlSigner> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(urlSigner);
        service = new DocumentAccessService(pointService, userMapper, documentMapper, unlockMapper,
                provider, agreementService, coverUrlService);
    }

    @Test
    void unlockedDocumentsUseSharedCoverSigning()
    {
        List<DocumentSummaryDto> documents = Collections.singletonList(new DocumentSummaryDto());
        when(userMapper.selectById(11L)).thenReturn(user(11L, 5L));
        when(unlockMapper.selectUnlockedDocuments(11L)).thenReturn(documents);

        assertEquals(documents, service.listUnlocked(11L));

        verify(coverUrlService).signCovers(documents);
    }

    @Test
    void duplicateUnlockChargesExactlyOnce()
    {
        WlDocument document = document();
        WlDocumentUnlock existing = unlock(11L, 22L, 20L, 31L);
        when(documentMapper.selectDocumentById(22L)).thenReturn(document);
        when(unlockMapper.selectUnlock(11L, 22L)).thenReturn(null, null, existing);
        when(unlockMapper.insertUnlock(any(WlDocumentUnlock.class))).thenReturn(1);
        WlWxUser locked = user(11L, 50L);
        when(userMapper.selectById(11L)).thenReturn(locked);
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(locked);
        WlPointRecord pointRecord = pointRecord(31L, 50L, 30L);
        when(pointService.deductAfterLock(eq(locked), eq(20L), eq("DOCUMENT_UNLOCK"),
                eq("DOCUMENT_UNLOCK:22:request-1"), any()))
                .thenReturn(pointRecord);

        DocumentUnlockResult first = service.unlock(11L, 22L, "request-1");
        DocumentUnlockResult second = service.unlock(11L, 22L, "request-2");

        assertEquals(20L, first.getSpentPoints());
        assertEquals(30L, first.getPointBalance());
        assertEquals(20L, second.getSpentPoints());
        verify(pointService, times(1)).deductAfterLock(eq(locked), eq(20L),
                eq("DOCUMENT_UNLOCK"), eq("DOCUMENT_UNLOCK:22:request-1"), any());
        verify(unlockMapper, times(1)).insertUnlock(any(WlDocumentUnlock.class));
    }

    @Test
    void disabledUserCannotReuseExistingUnlock()
    {
        WlWxUser disabled = user(11L, 30L);
        disabled.setStatus("1");
        when(userMapper.selectById(11L)).thenReturn(disabled);
        when(unlockMapper.selectUnlock(11L, 22L)).thenReturn(unlock(11L, 22L, 20L, 31L));

        assertEquals("当前账号已停用，请联系管理员", assertThrows(ServiceException.class,
                () -> service.unlock(11L, 22L, "request-2")).getMessage());
        verify(pointService, never()).deductAfterLock(any(), any(), any(), any(), any());
    }

    @Test
    void requestIdIsScopedToDocumentWhenChargingPoints()
    {
        WlWxUser locked = user(11L, 50L);
        when(userMapper.selectById(11L)).thenReturn(locked);
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(locked);
        when(documentMapper.selectDocumentById(22L)).thenReturn(document());
        WlDocument second = document();
        second.setId(23L);
        second.setTitle("第二份文档");
        when(documentMapper.selectDocumentById(23L)).thenReturn(second);
        when(unlockMapper.selectUnlock(any(), any())).thenReturn(null);
        when(unlockMapper.insertUnlock(any())).thenReturn(1);
        when(pointService.deductAfterLock(any(), any(), any(), any(), any()))
                .thenReturn(pointRecord(31L, 50L, 30L));

        service.unlock(11L, 22L, "reused-request");
        service.unlock(11L, 23L, "reused-request");

        verify(pointService).deductAfterLock(eq(locked), eq(20L), eq("DOCUMENT_UNLOCK"),
                eq("DOCUMENT_UNLOCK:22:reused-request"), any());
        verify(pointService).deductAfterLock(eq(locked), eq(20L), eq("DOCUMENT_UNLOCK"),
                eq("DOCUMENT_UNLOCK:23:reused-request"), any());
    }

    @Test
    void vipFreeDocumentUnlocksWithoutChargingActiveVip()
    {
        WlDocument document = document();
        document.setAccessType("VIP_FREE");
        WlWxUser locked = user(11L, 50L);
        locked.setVipExpireTime(new java.util.Date(System.currentTimeMillis() + 86400000L));
        when(userMapper.selectById(11L)).thenReturn(locked);
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(locked);
        when(documentMapper.selectDocumentById(22L)).thenReturn(document);
        when(unlockMapper.selectUnlock(11L, 22L)).thenReturn(null);
        when(unlockMapper.insertUnlock(any())).thenReturn(1);

        DocumentUnlockResult result = service.unlock(11L, 22L, "request-vip-free");

        assertEquals(0L, result.getSpentPoints());
        assertEquals(50L, result.getPointBalance());
        verify(pointService, never()).deductAfterLock(any(), any(), any(), any(), any());
        verify(unlockMapper).insertUnlock(any(WlDocumentUnlock.class));
    }

    @Test
    void vipFreeDocumentStillChargesPointsForNonVip()
    {
        WlDocument document = document();
        document.setAccessType("VIP_FREE");
        WlWxUser locked = user(11L, 50L);
        when(userMapper.selectById(11L)).thenReturn(locked);
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(locked);
        when(documentMapper.selectDocumentById(22L)).thenReturn(document);
        when(unlockMapper.selectUnlock(11L, 22L)).thenReturn(null);
        when(unlockMapper.insertUnlock(any())).thenReturn(1);
        when(pointService.deductAfterLock(eq(locked), eq(20L), eq("DOCUMENT_UNLOCK"),
                eq("DOCUMENT_UNLOCK:22:request-non-vip"), any())).thenReturn(pointRecord(31L, 50L, 30L));

        DocumentUnlockResult result = service.unlock(11L, 22L, "request-non-vip");

        assertEquals(20L, result.getSpentPoints());
        assertEquals(30L, result.getPointBalance());
        verify(pointService).deductAfterLock(eq(locked), eq(20L), eq("DOCUMENT_UNLOCK"),
                eq("DOCUMENT_UNLOCK:22:request-non-vip"), any());
    }

    @Test
    void zeroPointDocumentUnlocksWithoutPointRecord()
    {
        WlDocument document = document();
        document.setPointPrice(0L);
        WlWxUser locked = user(11L, 50L);
        when(userMapper.selectById(11L)).thenReturn(locked);
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(locked);
        when(documentMapper.selectDocumentById(22L)).thenReturn(document);
        when(unlockMapper.selectUnlock(11L, 22L)).thenReturn(null);
        when(unlockMapper.insertUnlock(any())).thenReturn(1);

        DocumentUnlockResult result = service.unlock(11L, 22L, "request-free", true);

        assertEquals(0L, result.getSpentPoints());
        assertEquals(50L, result.getPointBalance());
        verify(pointService, never()).deductAfterLock(any(), any(), any(), any(), any());
        verify(unlockMapper).insertUnlock(any(WlDocumentUnlock.class));
    }

    @Test
    void freeOnlyRequestNeverChargesExpiredMember()
    {
        WlDocument document = document();
        document.setAccessType("VIP_FREE");
        WlWxUser locked = user(11L, 50L);
        when(userMapper.selectById(11L)).thenReturn(locked);
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(locked);
        when(documentMapper.selectDocumentById(22L)).thenReturn(document);
        when(unlockMapper.selectUnlock(11L, 22L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.unlock(11L, 22L, "request-expired", true));

        assertEquals("当前文档不再满足免费获取条件，请刷新后重试", exception.getMessage());
        verify(pointService, never()).deductAfterLock(any(), any(), any(), any(), any());
        verify(unlockMapper, never()).insertUnlock(any());
    }

    @Test
    void previewSignsOnlyPreviewObjectKey() throws Exception
    {
        WlDocument document = document();
        when(documentMapper.selectDocumentById(22L)).thenReturn(document);
        when(userMapper.selectById(11L)).thenReturn(user(11L, 5L));
        URL url = new URL("https://temporary.example/preview");
        when(urlSigner.signGetUrl(eq("documents/preview.pdf"), any(Duration.class), eq(null)))
                .thenReturn(url);

        FileAuthorization result = service.authorizePreview(11L, 22L, "127.0.0.1");

        assertEquals(url.toString(), result.getUrl());
        verify(urlSigner).signGetUrl(eq("documents/preview.pdf"), any(Duration.class), eq(null));
        verify(urlSigner, never()).signGetUrl(eq("documents/original.docx"), any(), any());
    }

    @Test
    void previewRequiresStrictPageBoundary()
    {
        WlDocument document = document();
        document.setPreviewPages(0);
        when(documentMapper.selectDocumentById(22L)).thenReturn(document);
        when(userMapper.selectById(11L)).thenReturn(user(11L, 5L));

        assertEquals("文档试读页数配置不正确", assertThrows(ServiceException.class,
                () -> service.authorizePreview(11L, 22L, "127.0.0.1")).getMessage());
        verify(urlSigner, never()).signGetUrl(any(), any(), any());
    }

    @Test
    void originalFileRequiresUnlockBeforeDisclaimerAuthorization() throws Exception
    {
        when(documentMapper.selectDocumentById(22L)).thenReturn(document());
        when(userMapper.selectById(11L)).thenReturn(user(11L, 5L));
        when(unlockMapper.selectUnlock(11L, 22L)).thenReturn(null);

        assertEquals("请先兑换文档", assertThrows(ServiceException.class,
                () -> service.authorizeOriginalFile(11L, 22L, originalRequest(), "127.0.0.1")).getMessage());
        verify(agreementService, never()).validateFileDisclaimer(any(), any(), any());
        verify(urlSigner, never()).signGetUrl(any(), any(), any());
    }

    @Test
    void successfulSendRecordRequiresCurrentlyAvailableOriginalFile()
    {
        WlDocument document = document();
        document.setOriginalObjectKey("");
        when(documentMapper.selectDocumentById(22L)).thenReturn(document);
        when(userMapper.selectById(11L)).thenReturn(user(11L, 5L));
        when(unlockMapper.selectUnlock(11L, 22L)).thenReturn(unlock(11L, 22L, 20L, 31L));

        assertEquals("文档原文件暂不可用", assertThrows(ServiceException.class,
                () -> service.validateOriginalFileSendPermission(11L, 22L)).getMessage());
    }

    @Test
    void originalAuthorizationReturnsFilenameAndRecordsAction() throws Exception
    {
        WlDocument document = document();
        when(documentMapper.selectDocumentById(22L)).thenReturn(document);
        when(userMapper.selectById(11L)).thenReturn(user(11L, 5L));
        when(unlockMapper.selectUnlock(11L, 22L)).thenReturn(unlock(11L, 22L, 20L, 31L));
        URL url = new URL("https://temporary.example/original");
        when(urlSigner.signGetUrl(eq("documents/original.docx"), any(Duration.class),
                eq("质量管理手册.docx"))).thenReturn(url);

        FileAuthorization result = service.authorizeOriginalFile(11L, 22L, originalRequest(), "127.0.0.1");

        assertEquals("质量管理手册.docx", result.getFileName());
        assertEquals(url.toString(), result.getUrl());
        verify(agreementService).validateFileDisclaimer(eq(11L), any(OriginalFileRequest.class), eq("127.0.0.1"));
        verify(unlockMapper).insertView(11L, 22L, "ORIGINAL", "127.0.0.1");
    }

    @Test
    void fileDisclaimerRequiresEnabledUser()
    {
        WlWxUser disabled = user(11L, 5L);
        disabled.setStatus("1");
        when(userMapper.selectById(11L)).thenReturn(disabled);

        assertEquals("当前账号已停用，请联系管理员", assertThrows(ServiceException.class,
                () -> service.fileDisclaimer(11L)).getMessage());
        verify(agreementService, never()).fileDisclaimer(any());
    }

    private WlDocument document()
    {
        WlDocument document = new WlDocument();
        document.setId(22L);
        document.setTitle("质量管理手册");
        document.setFileFormat("DOCX");
        document.setPointPrice(20L);
        document.setPageCount(10);
        document.setPreviewPages(3);
        document.setPublishStatus("PUBLISHED");
        document.setConversionStatus("SUCCESS");
        document.setPreviewObjectKey("documents/preview.pdf");
        document.setFullObjectKey("documents/full.pdf");
        document.setOriginalObjectKey("documents/original.docx");
        return document;
    }

    private WlWxUser user(Long id, Long balance)
    {
        WlWxUser user = new WlWxUser();
        user.setId(id);
        user.setPointBalance(balance);
        user.setStatus("0");
        return user;
    }

    private WlDocumentUnlock unlock(Long userId, Long documentId, Long points, Long pointRecordId)
    {
        WlDocumentUnlock unlock = new WlDocumentUnlock();
        unlock.setId(41L);
        unlock.setUserId(userId);
        unlock.setDocumentId(documentId);
        unlock.setSpentPoints(points);
        unlock.setPointRecordId(pointRecordId);
        return unlock;
    }

    private WlPointRecord pointRecord(Long id, Long before, Long after)
    {
        WlPointRecord record = new WlPointRecord();
        record.setId(id);
        record.setBeforeBalance(before);
        record.setAfterBalance(after);
        record.setChangePoints(after - before);
        return record;
    }

    private OriginalFileRequest originalRequest()
    {
        OriginalFileRequest request = new OriginalFileRequest();
        request.setAgreementId(9L);
        request.setAgreementVersion("f1");
        request.setConfirmed(true);
        return request;
    }
}
