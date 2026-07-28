package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlBanner;
import com.ruoyi.library.dto.BannerImagePreviewResult;
import com.ruoyi.library.storage.BannerImageProcessor;
import com.ruoyi.library.storage.CosPrivateStorageService;
import com.ruoyi.library.storage.WechatProfileStoragePaths;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class BannerManagementServiceTest
{
    @TempDir
    Path tempDirectory;

    private CosPrivateStorageService storage;
    private DocumentService documentService;
    private RecordingTransactionManager transactionManager;
    private BannerManagementService service;

    @BeforeEach
    void setUp()
    {
        WechatProfileStoragePaths paths = mock(WechatProfileStoragePaths.class);
        when(paths.documentTempRoot()).thenReturn(tempDirectory.resolve("document-temp"));
        storage = mock(CosPrivateStorageService.class);
        documentService = mock(DocumentService.class);
        transactionManager = new RecordingTransactionManager();
        service = new BannerManagementService(new BannerImageProcessor(paths), storage,
                documentService, transactionManager);
        when(storage.putPrivateObject(anyString(), any(InputStream.class), anyLong(), eq("image/jpeg")))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void addDeletesNewObjectWhenDatabaseInsertFails() throws Exception
    {
        when(documentService.addBanner(any(WlBanner.class), eq("admin")))
                .thenThrow(new ServiceException("轮播图保存失败，请重试"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.add(request(null), validImage(), "admin"));

        assertEquals("轮播图保存失败，请重试", exception.getMessage());
        verify(storage).deleteObjectAfterMetadataDeletion(anyString());
        assertEquals(Arrays.asList("begin", "rollback"), transactionManager.events);
    }

    @Test
    void updateWithoutImagePreservesExistingObjectKey()
    {
        when(documentService.getBanner(4L)).thenReturn(stored(4L, "banners/old/image.jpg"));
        when(documentService.updateBanner(any(WlBanner.class), eq("banners/old/image.jpg"), eq("admin")))
                .thenReturn(1);
        WlBanner request = request(4L);
        request.setImageUrl("https://forged.example/image.jpg");

        assertEquals(1, service.update(request, null, "admin"));

        assertEquals("banners/old/image.jpg", request.getImageUrl());
        verify(documentService).updateBanner(request, "banners/old/image.jpg", "admin");
        verify(storage, never()).putPrivateObject(anyString(), any(), anyLong(), anyString());
        verify(storage, never()).deleteObjectAfterMetadataDeletion(anyString());
    }

    @Test
    void updateWithoutImageHidesUnexpectedPersistenceFailure()
    {
        when(documentService.getBanner(4L)).thenReturn(stored(4L, "banners/old/image.jpg"));
        when(documentService.updateBanner(any(WlBanner.class),
                eq("banners/old/image.jpg"), eq("admin")))
                .thenThrow(new IllegalStateException("database unavailable"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.update(request(4L), null, "admin"));

        assertEquals("轮播图保存失败，请重试", exception.getMessage());
    }

    @Test
    void updateDeletesNewObjectWhenTransactionFails() throws Exception
    {
        when(documentService.getBanner(4L)).thenReturn(stored(4L, "banners/old/image.jpg"));
        doThrow(new ServiceException("轮播图已发生变化，请刷新后重试"))
                .when(documentService).updateBanner(any(WlBanner.class),
                        eq("banners/old/image.jpg"), eq("admin"));

        assertThrows(ServiceException.class, () -> service.update(request(4L), validImage(), "admin"));

        verify(storage).deleteObjectAfterMetadataDeletion(org.mockito.ArgumentMatchers.argThat(
                key -> key.startsWith("banners/") && key.endsWith("/image.jpg")));
        verify(storage, never()).deleteObjectAfterMetadataDeletion("banners/old/image.jpg");
        assertEquals(Arrays.asList("begin", "rollback"), transactionManager.events);
    }

    @Test
    void updateDeletesOldObjectOnlyAfterCommit() throws Exception
    {
        when(documentService.getBanner(4L)).thenReturn(stored(4L, "banners/old/image.jpg"));
        when(documentService.updateBanner(any(WlBanner.class),
                eq("banners/old/image.jpg"), eq("admin"))).thenReturn(1);
        doAnswer(invocation -> {
            transactionManager.events.add("delete-old");
            return null;
        }).when(storage).deleteObjectAfterMetadataDeletion("banners/old/image.jpg");

        assertEquals(1, service.update(request(4L), validImage(), "admin"));

        assertTrue(transactionManager.events.indexOf("commit")
                < transactionManager.events.indexOf("delete-old"));
    }

    @Test
    void updateNeverDeletesLegacyHttpImage() throws Exception
    {
        when(documentService.getBanner(4L)).thenReturn(stored(4L, "https://old.example/banner.jpg"));
        when(documentService.updateBanner(any(WlBanner.class),
                eq("https://old.example/banner.jpg"), eq("admin"))).thenReturn(1);

        assertEquals(1, service.update(request(4L), validImage(), "admin"));

        verify(storage, never()).deleteObjectAfterMetadataDeletion("https://old.example/banner.jpg");
    }

    @Test
    void deleteRollsBackAllRowsWhenOneExpectedKeyChanged()
    {
        when(documentService.getBanner(4L)).thenReturn(stored(4L, "banners/4/image.jpg"));
        when(documentService.getBanner(5L)).thenReturn(stored(5L, "banners/5/image.jpg"));
        when(documentService.removeBanner(4L, "banners/4/image.jpg", "admin")).thenReturn(1);
        when(documentService.removeBanner(5L, "banners/5/image.jpg", "admin"))
                .thenThrow(new ServiceException("轮播图已发生变化，请刷新后重试"));

        assertThrows(ServiceException.class, () -> service.remove(new Long[] {4L, 5L}, "admin"));

        assertEquals(Arrays.asList("begin", "rollback"), transactionManager.events);
        verify(storage, never()).deleteObjectAfterMetadataDeletion(anyString());
    }

    @Test
    void deleteRemovesCosObjectsOnlyAfterCommit()
    {
        when(documentService.getBanner(4L)).thenReturn(stored(4L, "banners/4/image.jpg"));
        when(documentService.getBanner(5L)).thenReturn(stored(5L, "https://old.example/5.jpg"));
        when(documentService.removeBanner(anyLong(), anyString(), eq("admin"))).thenReturn(1);
        doAnswer(invocation -> {
            transactionManager.events.add("delete-object");
            return null;
        }).when(storage).deleteObjectAfterMetadataDeletion("banners/4/image.jpg");

        assertEquals(2, service.remove(new Long[] {4L, 5L}, "admin"));

        assertTrue(transactionManager.events.indexOf("commit")
                < transactionManager.events.indexOf("delete-object"));
        verify(storage, never()).deleteObjectAfterMetadataDeletion("https://old.example/5.jpg");
    }

    @Test
    void deleteHidesUnexpectedPersistenceFailure()
    {
        when(documentService.getBanner(4L)).thenReturn(stored(4L, "banners/4/image.jpg"));
        when(documentService.removeBanner(4L, "banners/4/image.jpg", "admin"))
                .thenThrow(new IllegalStateException("database unavailable"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.remove(new Long[] {4L}, "admin"));

        assertEquals("轮播图删除失败，请重试", exception.getMessage());
        verify(storage, never()).deleteObjectAfterMetadataDeletion(anyString());
    }

    @Test
    void previewSignsObjectKeyForThirtyMinutes() throws Exception
    {
        when(documentService.getBanner(4L)).thenReturn(stored(4L, "banners/4/image.jpg"));
        when(storage.signGetUrl("banners/4/image.jpg", Duration.ofMinutes(30), null))
                .thenReturn(new URL("https://signed.example/banner.jpg"));

        BannerImagePreviewResult result = service.preview(4L);

        assertEquals("https://signed.example/banner.jpg", result.getImageUrl());
        verify(storage).signGetUrl("banners/4/image.jpg", Duration.ofMinutes(30), null);
    }

    @Test
    void previewReturnsLegacyHttpImageWithoutSigning()
    {
        when(documentService.getBanner(4L)).thenReturn(stored(4L, "http://old.example/banner.jpg"));

        assertEquals("http://old.example/banner.jpg", service.preview(4L).getImageUrl());

        verify(storage, never()).signGetUrl(anyString(), any(Duration.class), isNull());
    }

    @Test
    void previewHidesStorageFailureDetails()
    {
        when(documentService.getBanner(4L)).thenReturn(stored(4L, "banners/4/image.jpg"));
        when(storage.signGetUrl("banners/4/image.jpg", Duration.ofMinutes(30), null))
                .thenThrow(new ServiceException("COS密钥未配置"));

        ServiceException exception = assertThrows(ServiceException.class, () -> service.preview(4L));

        assertEquals("轮播图图片服务暂不可用，请稍后重试", exception.getMessage());
    }

    private MockMultipartFile validImage() throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(BannerImageProcessor.WIDTH, BannerImageProcessor.HEIGHT,
                BufferedImage.TYPE_INT_RGB), "jpg", output);
        return new MockMultipartFile("image", "banner.jpg", "image/jpeg", output.toByteArray());
    }

    private WlBanner request(Long id)
    {
        WlBanner banner = new WlBanner();
        banner.setId(id);
        banner.setTitle("首页推荐");
        banner.setDocumentId(9L);
        banner.setStatus("0");
        banner.setSortOrder(0);
        return banner;
    }

    private WlBanner stored(Long id, String imageUrl)
    {
        WlBanner banner = request(id);
        banner.setImageUrl(imageUrl);
        return banner;
    }

    private static final class RecordingTransactionManager implements PlatformTransactionManager
    {
        private final List<String> events = new ArrayList<>();

        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition)
        {
            events.add("begin");
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status)
        {
            events.add("commit");
        }

        @Override
        public void rollback(TransactionStatus status)
        {
            events.add("rollback");
        }
    }
}
