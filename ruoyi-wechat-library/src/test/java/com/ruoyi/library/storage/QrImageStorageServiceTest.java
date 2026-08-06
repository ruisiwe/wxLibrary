package com.ruoyi.library.storage;

import com.ruoyi.common.exception.ServiceException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class QrImageStorageServiceTest
{
    private static final byte[] WEBP = Base64.getDecoder().decode(
            "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AA/vuUAAA=");

    @TempDir
    Path root;

    private QrImageStorageService service;

    @BeforeEach
    void setUp()
    {
        WechatProfileStoragePaths paths = mock(WechatProfileStoragePaths.class);
        when(paths.qrConfigRoot()).thenReturn(root.resolve("qr-config"));
        when(paths.vipCustomerServiceRoot()).thenReturn(root.resolve("vip-customer-service"));
        service = new QrImageStorageService(paths);
    }

    @Test
    void storesSupportedImagesInSeparatedDirectories() throws Exception
    {
        String qrPath = service.storeQrConfig(file("group.png", "image/png", image("png")));
        String vipPath = service.storeVipCustomerService(file("wechat.webp", "image/webp", WEBP));

        assertTrue(qrPath.matches("\\d{6}/[0-9a-f-]{36}\\.png"));
        assertTrue(vipPath.matches("\\d{6}/[0-9a-f-]{36}\\.webp"));
        assertTrue(service.resolveQrConfigForRead(qrPath).startsWith(root.resolve("qr-config")));
        assertTrue(service.resolveVipCustomerServiceForRead(vipPath)
                .startsWith(root.resolve("vip-customer-service")));
        assertFalse(qrPath.contains(root.toString()));
    }

    @Test
    void rejectsOversizedFakeAndMismatchedImages() throws Exception
    {
        assertThrows(ServiceException.class, () -> service.storeQrConfig(
                file("large.jpg", "image/jpeg", new byte[2 * 1024 * 1024 + 1])));
        assertThrows(ServiceException.class, () -> service.storeQrConfig(
                file("fake.jpg", "image/jpeg", new byte[] {1, 2, 3})));
        assertThrows(ServiceException.class, () -> service.storeQrConfig(
                file("wrong.jpg", "image/jpeg", image("png"))));
        assertThrows(ServiceException.class, () -> service.storeQrConfig(
                file("wrong.png", "image/jpeg", image("png"))));
    }

    @Test
    void rejectsTraversalAndCrossDirectoryReads() throws Exception
    {
        String vipPath = service.storeVipCustomerService(file("wechat.jpg", "image/jpeg", image("jpg")));

        assertThrows(ServiceException.class,
                () -> service.resolveQrConfigForRead("../../outside.jpg"));
        assertThrows(ServiceException.class,
                () -> service.resolveQrConfigForRead(root.resolve("absolute.jpg").toString()));
        assertThrows(ServiceException.class,
                () -> service.resolveQrConfigForRead(vipPath));
    }

    @Test
    void deletesOnlyFilesResolvedInsideSelectedDirectory() throws Exception
    {
        String qrPath = service.storeQrConfig(file("group.png", "image/png", image("png")));
        Path stored = service.resolveQrConfigForRead(qrPath);

        service.deleteVipCustomerServiceQuietly(qrPath);
        assertTrue(Files.exists(stored));

        service.deleteQrConfigQuietly(qrPath);
        assertFalse(Files.exists(stored));
    }

    private MockMultipartFile file(String name, String mime, byte[] content)
    {
        return new MockMultipartFile("image", name, mime, content);
    }

    private byte[] image(String format) throws Exception
    {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
