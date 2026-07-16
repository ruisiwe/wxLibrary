package com.ruoyi.library.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
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
        properties.setRootDirectory(root.toString());
        service = new AvatarStorageService(properties);
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
        properties.setRootDirectory(root.toString());
        properties.setMaxWidth(1);
        AvatarStorageService limited = new AvatarStorageService(properties);

        assertEquals("头像图片尺寸超出限制", assertThrows(ServiceException.class,
                () -> limited.store(file("a.png", "image/png", image("png")))).getMessage());
    }

    private MockMultipartFile file(String name, String mime, byte[] content)
    {
        return new MockMultipartFile("avatar", name, mime, content);
    }

    private byte[] image(String format) throws Exception
    {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(image, format, output);
        return output.toByteArray();
    }
}
