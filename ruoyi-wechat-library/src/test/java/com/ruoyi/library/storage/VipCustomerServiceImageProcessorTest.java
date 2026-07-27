package com.ruoyi.library.storage;

import com.ruoyi.common.exception.ServiceException;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VipCustomerServiceImageProcessorTest
{
    private static final byte[] WEBP = Base64.getDecoder().decode(
            "UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AA/vuUAAA=");

    @TempDir
    Path root;

    @Test
    void acceptsJpegPngAndWebpAndCleansTemporaryFile() throws Exception
    {
        VipCustomerServiceImageProcessor processor = new VipCustomerServiceImageProcessor(root);

        assertProcessed(processor, file("wechat.jpg", "image/jpeg", image("jpg")), "jpg", "image/jpeg");
        assertProcessed(processor, file("wechat.png", "image/png", image("png")), "png", "image/png");
        assertProcessed(processor, file("wechat.webp", "image/webp", WEBP), "webp", "image/webp");
    }

    @Test
    void rejectsOversizedUnsupportedAndContentMismatch() throws Exception
    {
        VipCustomerServiceImageProcessor processor = new VipCustomerServiceImageProcessor(root);

        assertEquals("客服微信图片不能超过2MB", assertThrows(ServiceException.class,
                () -> processor.process(file("wechat.jpg", "image/jpeg",
                        new byte[2 * 1024 * 1024 + 1]))).getMessage());
        assertEquals("客服微信图片仅支持JPEG、PNG或WebP格式", assertThrows(ServiceException.class,
                () -> processor.process(file("wechat.gif", "image/gif", image("png")))).getMessage());
        assertEquals("客服微信图片文件扩展名与实际内容不匹配", assertThrows(ServiceException.class,
                () -> processor.process(file("wechat.jpg", "image/jpeg", image("png")))).getMessage());
    }

    private void assertProcessed(VipCustomerServiceImageProcessor processor,
            MockMultipartFile file, String extension, String contentType) throws Exception
    {
        VipCustomerServiceImageProcessor.ProcessedImage processed = processor.process(file);
        Path stored = processed.getPath();
        assertTrue(Files.exists(stored));
        assertEquals(extension, processed.getExtension());
        assertEquals(contentType, processed.getContentType());
        assertTrue(processed.getSize() > 0);
        processed.close();
        assertFalse(Files.exists(stored));
    }

    private MockMultipartFile file(String name, String contentType, byte[] data)
    {
        return new MockMultipartFile("image", name, contentType, data);
    }

    private byte[] image(String format) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(120, 120, BufferedImage.TYPE_INT_RGB), format, output);
        return output.toByteArray();
    }
}
