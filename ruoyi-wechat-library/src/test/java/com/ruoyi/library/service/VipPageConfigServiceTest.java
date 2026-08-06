package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipBenefit;
import com.ruoyi.library.domain.WlVipPageConfig;
import com.ruoyi.library.dto.VipPageConfigView;
import com.ruoyi.library.mapper.WlVipPageConfigMapper;
import com.ruoyi.library.storage.QrImageStorageService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VipPageConfigServiceTest
{
    @TempDir
    Path root;

    private WlVipPageConfigMapper configMapper;
    private VipBenefitService benefitService;
    private QrImageStorageService storage;
    private RecordingTransactionManager transactionManager;
    private VipPageConfigService service;

    @BeforeEach
    void setUp()
    {
        configMapper = mock(WlVipPageConfigMapper.class);
        benefitService = mock(VipBenefitService.class);
        storage = mock(QrImageStorageService.class);
        transactionManager = new RecordingTransactionManager();
        service = new VipPageConfigService(configMapper, benefitService, storage, transactionManager);
    }

    @Test
    void rejectsBlankAndOverlongTip()
    {
        assertEquals("客服提示语不能为空", assertThrows(ServiceException.class,
                () -> service.update(request(" "), null, "admin")).getMessage());
        assertEquals("客服提示语不能超过100个字符", assertThrows(ServiceException.class,
                () -> service.update(request(repeat("客", 101)), null, "admin")).getMessage());
    }

    @Test
    void publicViewReturnsEnabledBenefitsAndControlledLocalImageUrl()
    {
        String path = "202607/customer.webp";
        when(configMapper.selectConfig()).thenReturn(stored(path));
        when(benefitService.listEnabled()).thenReturn(Arrays.asList(
                benefit("赠送积分"), benefit("VIP 文档免费下载")));
        when(storage.resolveVipCustomerServiceForRead(path))
                .thenReturn(root.resolve("vip-customer-service").resolve(path));

        VipPageConfigView view = service.getPublicView();

        assertEquals(Arrays.asList("赠送积分", "VIP 文档免费下载"), view.getBenefits());
        assertEquals("开通 VIP 请添加客服微信", view.getCustomerServiceTip());
        assertEquals("/wx/public/vip-page-config/customer-service-image",
                view.getCustomerServiceImageUrl());
    }

    @Test
    void legacyCosKeyIsTreatedAsNotConfigured()
    {
        String legacy = "vip/customer-service/old/wechat.webp";
        when(configMapper.selectConfig()).thenReturn(stored(legacy));
        when(storage.resolveVipCustomerServiceForRead(legacy))
                .thenThrow(new ServiceException("二维码图片不存在"));

        assertNull(service.getPublicView().getCustomerServiceImageUrl());
    }

    @Test
    void updateDeletesNewFileWhenDatabaseUpdateLosesRace()
    {
        when(configMapper.selectConfig()).thenReturn(stored("202607/old.png"));
        when(storage.storeVipCustomerService(any())).thenReturn("202607/new.png");
        when(configMapper.updateConfigWithExpectedImage(any(WlVipPageConfig.class),
                eq("202607/old.png"))).thenReturn(0);

        assertEquals("VIP 页面配置已发生变化，请刷新后重试", assertThrows(ServiceException.class,
                () -> service.update(request("开通 VIP 请添加客服微信"),
                        validPng(), "admin")).getMessage());

        verify(storage).deleteVipCustomerServiceQuietly("202607/new.png");
        verify(storage, never()).deleteVipCustomerServiceQuietly("202607/old.png");
        assertEquals(Arrays.asList("begin", "rollback"), transactionManager.events);
    }

    @Test
    void updateDeletesOldFileOnlyAfterCommit()
    {
        when(configMapper.selectConfig()).thenReturn(stored("202607/old.png"));
        when(storage.storeVipCustomerService(any())).thenReturn("202607/new.png");
        when(configMapper.updateConfigWithExpectedImage(any(WlVipPageConfig.class),
                eq("202607/old.png"))).thenReturn(1);
        doAnswer(invocation -> {
            transactionManager.events.add("delete-old");
            return null;
        }).when(storage).deleteVipCustomerServiceQuietly("202607/old.png");

        assertEquals(1, service.update(
                request("  开通 VIP 请添加客服微信  "), validPng(), " admin "));

        assertTrue(transactionManager.events.indexOf("commit")
                < transactionManager.events.indexOf("delete-old"));
    }

    @Test
    void clearImageCommitsBeforeDeletingLocalFile()
    {
        when(configMapper.selectConfig()).thenReturn(stored("202607/old.png"));
        when(configMapper.updateConfigWithExpectedImage(any(WlVipPageConfig.class),
                eq("202607/old.png"))).thenReturn(1);

        assertEquals(1, service.clearImage("admin"));

        verify(storage).deleteVipCustomerServiceQuietly("202607/old.png");
        assertEquals(Arrays.asList("begin", "commit"), transactionManager.events);
    }

    @Test
    void resolvesConfiguredCustomerServiceImageForControlledRead() throws Exception
    {
        Path file = root.resolve("customer.png");
        Files.write(file, new byte[] {1});
        when(configMapper.selectConfig()).thenReturn(stored("202607/customer.png"));
        when(storage.resolveVipCustomerServiceForRead("202607/customer.png")).thenReturn(file);

        assertEquals(file, service.resolveCustomerServiceImageForRead());
    }

    private WlVipPageConfig request(String tip)
    {
        WlVipPageConfig config = new WlVipPageConfig();
        config.setCustomerServiceTip(tip);
        return config;
    }

    private WlVipPageConfig stored(String imageKey)
    {
        WlVipPageConfig config = request("开通 VIP 请添加客服微信");
        config.setId(1L);
        config.setCustomerServiceImageKey(imageKey);
        return config;
    }

    private WlVipBenefit benefit(String text)
    {
        WlVipBenefit benefit = new WlVipBenefit();
        benefit.setBenefitText(text);
        return benefit;
    }

    private MockMultipartFile validPng()
    {
        return new MockMultipartFile("image", "wechat.png", "image/png", new byte[] {1});
    }

    private String repeat(String value, int count)
    {
        StringBuilder text = new StringBuilder();
        for (int index = 0; index < count; index++) text.append(value);
        return text.toString();
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
