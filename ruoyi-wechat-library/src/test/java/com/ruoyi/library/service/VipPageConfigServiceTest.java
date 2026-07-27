package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipBenefit;
import com.ruoyi.library.domain.WlVipPageConfig;
import com.ruoyi.library.dto.VipPageConfigView;
import com.ruoyi.library.mapper.WlVipPageConfigMapper;
import com.ruoyi.library.storage.CosPrivateStorageService;
import com.ruoyi.library.storage.VipCustomerServiceImageProcessor;
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
    private CosPrivateStorageService storage;
    private RecordingTransactionManager transactionManager;
    private VipPageConfigService service;

    @BeforeEach
    void setUp()
    {
        configMapper = mock(WlVipPageConfigMapper.class);
        benefitService = mock(VipBenefitService.class);
        storage = mock(CosPrivateStorageService.class);
        transactionManager = new RecordingTransactionManager();
        service = new VipPageConfigService(configMapper, benefitService,
                new VipCustomerServiceImageProcessor(root), storage, transactionManager);
        when(storage.putPrivateObject(anyString(), any(InputStream.class), anyLong(), anyString()))
                .thenAnswer(invocation -> invocation.getArgument(0));
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
    void publicViewReturnsEnabledBenefitsAndSignedImage() throws Exception
    {
        when(configMapper.selectConfig()).thenReturn(stored("vip/customer-service/old/wechat.webp"));
        when(benefitService.listEnabled()).thenReturn(Arrays.asList(
                benefit("赠送积分"), benefit("VIP 文档免费下载")));
        when(storage.signGetUrl("vip/customer-service/old/wechat.webp",
                Duration.ofMinutes(30), null))
                .thenReturn(new URL("https://signed.example/customer.webp"));

        VipPageConfigView view = service.getPublicView();

        assertEquals(Arrays.asList("赠送积分", "VIP 文档免费下载"), view.getBenefits());
        assertEquals("开通 VIP 请添加客服微信", view.getCustomerServiceTip());
        assertEquals("https://signed.example/customer.webp", view.getCustomerServiceImageUrl());
    }

    @Test
    void updateDeletesNewObjectWhenDatabaseUpdateLosesRace() throws Exception
    {
        when(configMapper.selectConfig()).thenReturn(stored("vip/customer-service/old/wechat.png"));
        when(configMapper.updateConfigWithExpectedImage(any(WlVipPageConfig.class),
                eq("vip/customer-service/old/wechat.png"))).thenReturn(0);

        assertEquals("VIP 页面配置已发生变化，请刷新后重试", assertThrows(ServiceException.class,
                () -> service.update(request("开通 VIP 请添加客服微信"),
                        validPng(), "admin")).getMessage());

        verify(storage).deleteObjectAfterMetadataDeletion(
                org.mockito.ArgumentMatchers.argThat(key ->
                        key.startsWith("vip/customer-service/") && key.endsWith("/wechat.png")));
        verify(storage, never()).deleteObjectAfterMetadataDeletion("vip/customer-service/old/wechat.png");
        assertEquals(Arrays.asList("begin", "commit"), transactionManager.events);
    }

    @Test
    void updateDeletesOldObjectOnlyAfterCommit() throws Exception
    {
        when(configMapper.selectConfig()).thenReturn(stored("vip/customer-service/old/wechat.png"));
        when(configMapper.updateConfigWithExpectedImage(any(WlVipPageConfig.class),
                eq("vip/customer-service/old/wechat.png"))).thenReturn(1);
        doAnswer(invocation -> {
            transactionManager.events.add("delete-old");
            return null;
        }).when(storage).deleteObjectAfterMetadataDeletion("vip/customer-service/old/wechat.png");

        assertEquals(1, service.update(request("  开通 VIP 请添加客服微信  "), validPng(), " admin "));

        assertTrue(transactionManager.events.indexOf("commit")
                < transactionManager.events.indexOf("delete-old"));
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

    private MockMultipartFile validPng() throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(120, 120, BufferedImage.TYPE_INT_RGB), "png", output);
        return new MockMultipartFile("image", "wechat.png", "image/png", output.toByteArray());
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
