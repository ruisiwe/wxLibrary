package com.ruoyi.library.storage;

import com.ruoyi.common.exception.ServiceException;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** 客服微信图片格式、安全性和临时文件处理器。 */
@Component
public class VipCustomerServiceImageProcessor
{
    public static final long MAX_BYTES = 2L * 1024L * 1024L;

    private static final Map<String, String> MIME_BY_EXTENSION = new HashMap<>();
    static
    {
        MIME_BY_EXTENSION.put("jpg", "image/jpeg");
        MIME_BY_EXTENSION.put("jpeg", "image/jpeg");
        MIME_BY_EXTENSION.put("png", "image/png");
        MIME_BY_EXTENSION.put("webp", "image/webp");
    }

    private final Path root;

    public VipCustomerServiceImageProcessor()
    {
        this(Paths.get(System.getProperty("java.io.tmpdir"), "wl-vip-customer-images"));
    }

    public VipCustomerServiceImageProcessor(Path root)
    {
        this.root = root.toAbsolutePath().normalize();
    }

    /** 校验客服微信图片并返回自动清理的临时文件。 */
    public ProcessedImage process(MultipartFile file)
    {
        if (file == null || file.isEmpty()) throw new ServiceException("客服微信图片不能为空");
        if (file.getSize() > MAX_BYTES) throw new ServiceException("客服微信图片不能超过2MB");
        String extension = extension(file.getOriginalFilename());
        String expectedMime = MIME_BY_EXTENSION.get(extension);
        if (expectedMime == null) throw new ServiceException("客服微信图片仅支持JPEG、PNG或WebP格式");
        if (!expectedMime.equalsIgnoreCase(trim(file.getContentType())))
            throw new ServiceException("客服微信图片文件扩展名与类型不匹配");

        Path temp = null;
        try
        {
            Files.createDirectories(root);
            temp = Files.createTempFile(root, "wl-vip-customer-", ".tmp");
            file.transferTo(temp.toFile());
            long size = Files.size(temp);
            if (size < 1) throw new ServiceException("客服微信图片不能为空");
            if (size > MAX_BYTES) throw new ServiceException("客服微信图片不能超过2MB");
            validateImage(temp, extension);
            return new ProcessedImage(temp, size, expectedMime, normalizeExtension(extension));
        }
        catch (ServiceException exception)
        {
            deleteQuietly(temp);
            throw exception;
        }
        catch (IOException | RuntimeException exception)
        {
            deleteQuietly(temp);
            throw new ServiceException("客服微信图片处理失败，请稍后重试");
        }
    }

    private void validateImage(Path file, String expectedExtension) throws IOException
    {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile()))
        {
            if (input == null) throw new ServiceException("客服微信图片内容不是有效图片");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ServiceException("客服微信图片内容不是有效图片");
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(input, true, true);
                if (!normalizeFormat(expectedExtension).equals(normalizeFormat(reader.getFormatName())))
                    throw new ServiceException("客服微信图片文件扩展名与实际内容不匹配");
                if (reader.getWidth(0) <= 0 || reader.getHeight(0) <= 0)
                    throw new ServiceException("客服微信图片内容不是有效图片");
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw new ServiceException("客服微信图片内容不是有效图片");
            }
            catch (ServiceException exception)
            {
                throw exception;
            }
            catch (IOException | RuntimeException exception)
            {
                throw new ServiceException("客服微信图片内容不是有效图片");
            }
            finally
            {
                reader.dispose();
            }
        }
    }

    private String extension(String filename)
    {
        if (filename == null || filename.lastIndexOf('.') < 0) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeExtension(String extension)
    {
        return "jpeg".equals(extension) ? "jpg" : extension;
    }

    private String normalizeFormat(String format)
    {
        String normalized = format.toLowerCase(Locale.ROOT);
        return "jpg".equals(normalized) ? "jpeg" : normalized;
    }

    private String trim(String value)
    {
        return value == null ? null : value.trim();
    }

    private void deleteQuietly(Path file)
    {
        if (file == null) return;
        try { Files.deleteIfExists(file); }
        catch (IOException ignored) { /* 临时文件由系统后续回收。 */ }
    }

    /** 已校验的客服图片临时文件，使用后必须关闭。 */
    public static final class ProcessedImage implements AutoCloseable
    {
        private final Path path;
        private final long size;
        private final String contentType;
        private final String extension;
        private boolean closed;

        private ProcessedImage(Path path, long size, String contentType, String extension)
        {
            this.path = path;
            this.size = size;
            this.contentType = contentType;
            this.extension = extension;
        }

        public InputStream openStream()
        {
            if (closed) throw new ServiceException("客服微信图片临时文件已失效");
            try { return Files.newInputStream(path); }
            catch (IOException exception) { throw new ServiceException("客服微信图片临时文件读取失败"); }
        }

        public Path getPath() { return path; }
        public long getSize() { return size; }
        public String getContentType() { return contentType; }
        public String getExtension() { return extension; }

        @Override
        public void close()
        {
            closed = true;
            try { Files.deleteIfExists(path); }
            catch (IOException ignored) { /* 临时文件由系统后续回收。 */ }
        }
    }
}
