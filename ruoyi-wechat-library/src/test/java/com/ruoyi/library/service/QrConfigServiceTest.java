package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlQrConfig;
import com.ruoyi.library.dto.QrConfigView;
import com.ruoyi.library.mapper.WlQrConfigMapper;
import com.ruoyi.library.storage.QrImageStorageService;
import com.github.pagehelper.Page;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QrConfigServiceTest
{
    private WlQrConfigMapper mapper;
    private QrImageStorageService storage;
    private RecordingTransactionManager transactionManager;
    private QrConfigService service;

    @BeforeEach
    void setUp()
    {
        mapper = mock(WlQrConfigMapper.class);
        storage = mock(QrImageStorageService.class);
        transactionManager = new RecordingTransactionManager();
        service = new QrConfigService(mapper, storage, transactionManager);
    }

    @Test
    void enabledListReturnsSortedSafeViews()
    {
        when(mapper.selectEnabled()).thenReturn(Arrays.asList(
                stored(2L, "加入社群", null), stored(3L, "关注视频号", "202607/video.png")));

        List<QrConfigView> result = service.listEnabled();

        assertEquals(2, result.size());
        assertEquals("加入社群", result.get(0).getMenuName());
        assertFalse(result.get(0).isImageConfigured());
        assertTrue(result.get(1).isImageConfigured());
        assertEquals("/wx/qr-configs/3/image", result.get(1).getImageUrl());
    }

    @Test
    void managementListPreservesPageHelperTotal()
    {
        Page<WlQrConfig> page = new Page<>(2, 10);
        page.setTotal(25);
        page.add(stored(2L, "加入社群", null));
        when(mapper.selectList(any(WlQrConfig.class))).thenReturn(page);

        List<QrConfigView> result = service.list(new WlQrConfig());

        assertTrue(result instanceof Page);
        assertEquals(25, ((Page<?>) result).getTotal());
        assertEquals(2, ((Page<?>) result).getPageNum());
    }

    @Test
    void validatesAndNormalizesManagementFields()
    {
        WlQrConfig request = new WlQrConfig();
        request.setMenuName("  加入社群  ");
        request.setGuideText("  长按识别二维码  ");
        request.setStatus("0");
        when(mapper.insertConfig(request)).thenReturn(1);

        assertEquals(1, service.add(request, "admin"));
        assertEquals("加入社群", request.getMenuName());
        assertEquals("长按识别二维码", request.getGuideText());
        assertEquals(0, request.getSortOrder());
        assertEquals("admin", request.getCreateBy());

        request.setSortOrder(-1);
        assertThrows(ServiceException.class, () -> service.edit(request, "admin"));
    }

    @Test
    void uploadDeletesNewFileWhenOptimisticDatabaseUpdateFails()
    {
        WlQrConfig current = stored(5L, "加入社群", "202607/old.png");
        when(mapper.selectById(5L)).thenReturn(current);
        when(storage.storeQrConfig(any())).thenReturn("202607/new.png");
        when(mapper.updateImageWithExpectedPath(
                5L, "202607/new.png", "202607/old.png", "admin")).thenReturn(0);

        assertThrows(ServiceException.class,
                () -> service.uploadImage(5L, image(), "admin"));

        verify(storage).deleteQrConfigQuietly("202607/new.png");
        verify(storage, never()).deleteQrConfigQuietly("202607/old.png");
        assertEquals(Arrays.asList("begin", "rollback"), transactionManager.events);
    }

    @Test
    void uploadDeletesOldFileOnlyAfterDatabaseCommit()
    {
        WlQrConfig current = stored(5L, "加入社群", "202607/old.png");
        when(mapper.selectById(5L)).thenReturn(current);
        when(storage.storeQrConfig(any())).thenReturn("202607/new.png");
        when(mapper.updateImageWithExpectedPath(
                5L, "202607/new.png", "202607/old.png", "admin")).thenReturn(1);
        doAnswer(invocation -> {
            transactionManager.events.add("delete-old");
            return null;
        }).when(storage).deleteQrConfigQuietly("202607/old.png");

        assertEquals(1, service.uploadImage(5L, image(), "admin"));

        assertTrue(transactionManager.events.indexOf("commit")
                < transactionManager.events.indexOf("delete-old"));
    }

    @Test
    void clearAndDeleteRemoveOldFileOnlyAfterDatabaseCommit()
    {
        WlQrConfig current = stored(5L, "加入社群", "202607/old.png");
        when(mapper.selectById(5L)).thenReturn(current);
        when(mapper.updateImageWithExpectedPath(
                5L, null, "202607/old.png", "admin")).thenReturn(1);

        assertEquals(1, service.clearImage(5L, "admin"));
        verify(storage).deleteQrConfigQuietly("202607/old.png");

        transactionManager.events.clear();
        when(mapper.deleteConfigWithExpectedPath(
                5L, "202607/old.png", "admin")).thenReturn(1);
        assertEquals(1, service.remove(5L, "admin"));
        assertEquals(Arrays.asList("begin", "commit"), transactionManager.events);
    }

    @Test
    void enabledDetailRejectsDisabledConfiguration()
    {
        WlQrConfig disabled = stored(8L, "内部群", null);
        disabled.setStatus("1");
        when(mapper.selectById(8L)).thenReturn(disabled);

        assertThrows(ServiceException.class, () -> service.getEnabled(8L));
    }

    private MockMultipartFile image()
    {
        return new MockMultipartFile("image", "qr.png", "image/png", new byte[] {1});
    }

    private WlQrConfig stored(Long id, String menuName, String imagePath)
    {
        WlQrConfig config = new WlQrConfig();
        config.setId(id);
        config.setMenuName(menuName);
        config.setGuideText("长按识别二维码");
        config.setImagePath(imagePath);
        config.setSortOrder(10);
        config.setStatus("0");
        return config;
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
