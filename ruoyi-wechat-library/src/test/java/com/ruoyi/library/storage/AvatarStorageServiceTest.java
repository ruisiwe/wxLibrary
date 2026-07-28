package com.ruoyi.library.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import javax.imageio.ImageIO;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.AvatarStorageProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AvatarStorageServiceTest
{
    private static final byte[] WEBP = Base64.getDecoder().decode(
            "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AA/vuUAAA=");

    @TempDir
    Path root;

    private AvatarStorageService service;

    @BeforeEach
    void setUp()
    {
        AvatarStorageProperties properties = new AvatarStorageProperties();
        service = service(properties, avatarRoot());
    }

    @Test
    void storesDecodableJpegPngAndWebpUsingRelativePaths() throws Exception
    {
        String jpeg = service.store(file("a.jpg", "image/jpeg", image("jpg")));
        String png = service.store(file("a.png", "image/png", image("png")));
        String webp = service.store(file("a.webp", "image/webp", WEBP));

        assertTrue(jpeg.matches("\\d{6}/[0-9a-f-]{36}\\.jpg"));
        assertTrue(png.endsWith(".png"));
        assertTrue(webp.endsWith(".webp"));
        assertFalse(jpeg.contains(root.toString()));
        assertFalse(jpeg.startsWith("avatar/"));
        Path storedJpeg = avatarRoot().resolve(jpeg);
        assertTrue(Files.exists(storedJpeg));
        assertTrue(Files.isRegularFile(storedJpeg, LinkOption.NOFOLLOW_LINKS));
        assertTrue(Files.isRegularFile(service.resolveForRead(jpeg)));
    }

    @Test
    void rejectsExtensionMimeAndContentMismatches() throws Exception
    {
        assertEquals("头像文件扩展名与类型不匹配", assertThrows(ServiceException.class,
                () -> service.store(file("a.png", "image/jpeg", image("png")))).getMessage());
        assertEquals("头像文件内容不是有效图片", assertThrows(ServiceException.class,
                () -> service.store(file("a.jpg", "image/jpeg", new byte[] {1, 2, 3}))).getMessage());
        assertEquals("头像文件扩展名与实际内容不匹配", assertThrows(ServiceException.class,
                () -> service.store(file("a.jpg", "image/jpeg", image("png")))).getMessage());
    }

    @Test
    void rejectsOversizedFileAndTraversal()
    {
        byte[] tooLarge = new byte[2 * 1024 * 1024 + 1];
        assertEquals("头像文件不能超过2MB", assertThrows(ServiceException.class,
                () -> service.store(file("a.jpg", "image/jpeg", tooLarge))).getMessage());
        assertEquals("头像路径不合法", assertThrows(ServiceException.class,
                () -> service.resolveForRead("../../outside.jpg")).getMessage());
        assertEquals("头像路径不合法", assertThrows(ServiceException.class,
                () -> service.resolveForRead(root.resolve("absolute.jpg").toString())).getMessage());
    }

    @Test
    void rejectsImagesBeyondConfiguredPixelDimensions() throws Exception
    {
        AvatarStorageProperties properties = new AvatarStorageProperties();
        properties.setMaxWidth(1);
        AvatarStorageService limited = service(properties, avatarRoot());

        assertEquals("头像图片尺寸超出限制", assertThrows(ServiceException.class,
                () -> limited.store(file("a.png", "image/png", image("png")))).getMessage());
    }

    @Test
    void hardLimitCannotBeExpandedByExternalConfiguration()
    {
        AvatarStorageProperties properties = new AvatarStorageProperties();
        properties.setMaxBytes(10L * 1024 * 1024);
        AvatarStorageService configuredLarger = service(properties, avatarRoot());
        byte[] tooLarge = new byte[2 * 1024 * 1024 + 1];

        assertEquals("头像文件不能超过2MB", assertThrows(ServiceException.class,
                () -> configuredLarger.store(file("a.jpg", "image/jpeg", tooLarge))).getMessage());
    }

    @Test
    void imageDimensionHardLimitCannotBeExpandedByExternalConfiguration() throws Exception
    {
        AvatarStorageProperties properties = new AvatarStorageProperties();
        properties.setMaxWidth(10000);
        properties.setMaxHeight(10000);
        properties.setMaxPixels(100000000L);
        AvatarStorageService configuredLarger = service(properties, avatarRoot());
        BufferedImage image = new BufferedImage(2049, 1, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, "png", output);

        assertEquals("头像图片尺寸超出限制", assertThrows(ServiceException.class,
                () -> configuredLarger.store(file("a.png", "image/png", output.toByteArray()))).getMessage());
    }

    @Test
    void rejectsSymbolicLinkStorageRoot() throws Exception
    {
        Path target = Files.createDirectory(root.resolve("target"));
        Path link = root.resolve("link");
        try
        {
            Files.createSymbolicLink(link, target);
        }
        catch (UnsupportedOperationException | java.io.IOException | SecurityException exception)
        {
            assumeTrue(false, "当前环境不允许创建符号链接");
        }
        AvatarStorageProperties properties = new AvatarStorageProperties();
        AvatarStorageService linked = service(properties, link);

        assertEquals("头像存储目录不合法", assertThrows(ServiceException.class,
                () -> linked.store(file("a.png", "image/png", image("png")))).getMessage());
    }

    @Test
    void rejectsSymbolicLinkMonthDirectory() throws Exception
    {
        String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
        Path target = Files.createDirectory(root.resolve("month-target"));
        Files.createDirectories(avatarRoot());
        Path link = avatarRoot().resolve(month);
        try
        {
            Files.createSymbolicLink(link, target);
        }
        catch (UnsupportedOperationException | java.io.IOException | SecurityException exception)
        {
            assumeTrue(false, "当前环境不允许创建符号链接");
        }

        assertEquals("头像存储目录不合法", assertThrows(ServiceException.class,
                () -> service.store(file("a.png", "image/png", image("png")))).getMessage());
    }

    @Test
    void rejectsSymbolicLinkAvatarDuringRead() throws Exception
    {
        Files.createDirectories(avatarRoot());
        Path target = avatarRoot().resolve("target.png");
        Files.write(target, image("png"));
        Path link = avatarRoot().resolve("link.png");
        try
        {
            Files.createSymbolicLink(link, target);
        }
        catch (UnsupportedOperationException | java.io.IOException | SecurityException exception)
        {
            assumeTrue(false, "当前环境不允许创建符号链接");
        }

        assertEquals("头像路径不合法", assertThrows(ServiceException.class,
                () -> service.resolveForRead("link.png")).getMessage());
    }

    @Test
    void rechecksActualTemporaryFileSizeAndRejectsInvalidLimitConfiguration() throws Exception
    {
        AvatarStorageProperties invalid = new AvatarStorageProperties();
        invalid.setMaxBytes(0);
        AvatarStorageService invalidService = service(invalid, avatarRoot());
        assertEquals("头像文件大小配置必须大于0", assertThrows(ServiceException.class,
                () -> invalidService.store(file("a.jpg", "image/jpeg", new byte[] {1}))).getMessage());

        org.springframework.web.multipart.MultipartFile misleading =
                mock(org.springframework.web.multipart.MultipartFile.class);
        when(misleading.isEmpty()).thenReturn(false);
        when(misleading.getSize()).thenReturn(1L);
        when(misleading.getOriginalFilename()).thenReturn("a.jpg");
        when(misleading.getContentType()).thenReturn("image/jpeg");
        doAnswer(invocation -> {
            java.io.File target = invocation.getArgument(0);
            Files.write(target.toPath(), new byte[2 * 1024 * 1024 + 1]);
            return null;
        }).when(misleading).transferTo(org.mockito.ArgumentMatchers.any(java.io.File.class));

        assertEquals("头像文件不能超过2MB", assertThrows(ServiceException.class,
                () -> service.store(misleading)).getMessage());
    }

    private MockMultipartFile file(String name, String mime, byte[] content)
    {
        return new MockMultipartFile("avatar", name, mime, content);
    }

    private Path avatarRoot()
    {
        return root.resolve("avatar");
    }

    private AvatarStorageService service(AvatarStorageProperties properties, Path configuredRoot)
    {
        WechatProfileStoragePaths paths = mock(WechatProfileStoragePaths.class);
        when(paths.avatarRoot()).thenReturn(configuredRoot);
        return new AvatarStorageService(properties, paths);
    }

    private byte[] image(String format) throws Exception
    {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
