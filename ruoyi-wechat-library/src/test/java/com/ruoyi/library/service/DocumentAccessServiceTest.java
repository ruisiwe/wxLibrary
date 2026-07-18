package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.domain.WlDocumentUnlock;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.DocumentUnlockResult;
import com.ruoyi.library.dto.FileAuthorization;
import com.ruoyi.library.mapper.WlDocumentMapper;
import com.ruoyi.library.mapper.WlDocumentUnlockMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.storage.PrivateFileUrlSigner;
import java.net.URL;
import java.time.Duration;
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
        ObjectProvider<PrivateFileUrlSigner> provider = mock(ObjectProvider.class);
        when(provider.getIfAvailable()).thenReturn(urlSigner);
        service = new DocumentAccessService(pointService, userMapper, documentMapper, unlockMapper, provider);
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
        when(pointService.deductAfterLock(eq(locked), eq(20L), eq("DOCUMENT_UNLOCK"), eq("request-1"), any()))
                .thenReturn(pointRecord);

        DocumentUnlockResult first = service.unlock(11L, 22L, "request-1");
        DocumentUnlockResult second = service.unlock(11L, 22L, "request-2");

        assertEquals(20L, first.getSpentPoints());
        assertEquals(30L, first.getPointBalance());
        assertEquals(20L, second.getSpentPoints());
        verify(pointService, times(1)).deductAfterLock(eq(locked), eq(20L),
                eq("DOCUMENT_UNLOCK"), eq("request-1"), any());
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
    void fullAndOriginalFilesRequireUnlock() throws Exception
    {
        when(documentMapper.selectDocumentById(22L)).thenReturn(document());
        when(userMapper.selectById(11L)).thenReturn(user(11L, 5L));
        when(unlockMapper.selectUnlock(11L, 22L)).thenReturn(null);

        assertEquals("请先兑换文档", assertThrows(ServiceException.class,
                () -> service.authorizeFullDocument(11L, 22L, "127.0.0.1")).getMessage());
        assertEquals("请先兑换文档", assertThrows(ServiceException.class,
                () -> service.authorizeOriginalFile(11L, 22L, "127.0.0.1")).getMessage());
        verify(urlSigner, never()).signGetUrl(any(), any(), any());
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

        FileAuthorization result = service.authorizeOriginalFile(11L, 22L, "127.0.0.1");

        assertEquals("质量管理手册.docx", result.getFileName());
        assertEquals(url.toString(), result.getUrl());
        verify(unlockMapper).insertView(11L, 22L, "ORIGINAL", "127.0.0.1");
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
}
