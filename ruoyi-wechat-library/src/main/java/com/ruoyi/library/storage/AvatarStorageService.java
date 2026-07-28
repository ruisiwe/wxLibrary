package com.ruoyi.library.storage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.AvatarStorageProperties;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 本地头像安全存储服务。 */
@Service
public class AvatarStorageService
{
    private static final long HARD_MAX_BYTES = 2L * 1024 * 1024;
    private static final int HARD_MAX_WIDTH = 2048;
    private static final int HARD_MAX_HEIGHT = 2048;
    private static final long HARD_MAX_PIXELS = 4194304L;
    private static final Map<String, String> MIME_BY_EXTENSION = new HashMap<>();
    static
    {
        MIME_BY_EXTENSION.put("jpg", "image/jpeg");
        MIME_BY_EXTENSION.put("jpeg", "image/jpeg");
        MIME_BY_EXTENSION.put("png", "image/png");
        MIME_BY_EXTENSION.put("webp", "image/webp");
    }

    private final AvatarStorageProperties properties;
    private final Path root;

    public AvatarStorageService(AvatarStorageProperties properties, WechatProfileStoragePaths paths)
    {
        this.properties = properties;
        this.root = paths.avatarRoot();
    }

    /** 校验并原子保存头像，返回相对路径。 */
    public String store(MultipartFile file)
    {
        ensureConfigured();
        long maxBytes = effectiveMaxBytes();
        if (file == null || file.isEmpty()) throw new ServiceException("首次登录必须上传有效头像");
        if (file.getSize() > maxBytes) throw fileTooLarge(maxBytes);
        String extension = extension(file.getOriginalFilename());
        String expectedMime = MIME_BY_EXTENSION.get(extension);
        if (expectedMime == null) throw new ServiceException("头像仅支持JPEG、PNG或WebP格式");
        if (!expectedMime.equalsIgnoreCase(trim(file.getContentType())))
            throw new ServiceException("头像文件扩展名与类型不匹配");

        Path temp = null;
        try
        {
            rejectSymbolicLink(root);
            Files.createDirectories(root);
            rejectSymbolicLink(root);
            temp = Files.createTempFile(root, ".avatar-", ".tmp");
            file.transferTo(temp.toFile());
            if (Files.size(temp) > maxBytes) throw fileTooLarge(maxBytes);
            validateImage(temp, extension);
            String month = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMM"));
            Path directory = root.resolve(month);
            rejectSymbolicLink(directory);
            Files.createDirectories(directory);
            rejectSymbolicLink(directory);
            String storedExtension = "jpeg".equals(extension) ? "jpg" : extension;
            Path target = directory.resolve(UUID.randomUUID().toString() + "." + storedExtension);
            Files.move(temp, target, StandardCopyOption.ATOMIC_MOVE);
            return month + "/" + target.getFileName().toString();
        }
        catch (ServiceException exception)
        {
            throw exception;
        }
        catch (IOException exception)
        {
            throw new ServiceException("头像保存失败，请稍后重试");
        }
        finally
        {
            if (temp != null)
            {
                try { Files.deleteIfExists(temp); }
                catch (IOException ignored) { /* 临时文件清理由系统后续回收。 */ }
            }
        }
    }

    /** 将数据库中的相对头像路径解析为根目录内的受控文件。 */
    public Path resolveForRead(String relativePath)
    {
        ensureConfigured();
        if (isBlank(relativePath)) throw new ServiceException("头像路径不合法");
        Path relative;
        try { relative = Paths.get(relativePath); }
        catch (RuntimeException exception) { throw new ServiceException("头像路径不合法"); }
        if (relative.isAbsolute()) throw new ServiceException("头像路径不合法");
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) throw new ServiceException("头像路径不合法");
        rejectSymbolicLink(root);
        rejectSymbolicLinkComponents(resolved);
        if (!Files.isRegularFile(resolved, LinkOption.NOFOLLOW_LINKS))
            throw new ServiceException("头像路径不合法");
        return resolved;
    }

    public void deleteQuietly(String relativePath)
    {
        try { Files.deleteIfExists(resolveForRead(relativePath)); }
        catch (RuntimeException | IOException ignored) { /* 清理失败不覆盖主业务异常。 */ }
    }

    private void validateImage(Path file, String expectedExtension) throws IOException
    {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile()))
        {
            if (input == null) throw new ServiceException("头像文件内容不是有效图片");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ServiceException("头像文件内容不是有效图片");
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(input, true, true);
                String actual = normalizeFormat(reader.getFormatName());
                String expected = normalizeFormat(expectedExtension);
                if (!expected.equals(actual)) throw new ServiceException("头像文件扩展名与实际内容不匹配");
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                long pixels = (long) width * height;
                int maxWidth = Math.min(properties.getMaxWidth(), HARD_MAX_WIDTH);
                int maxHeight = Math.min(properties.getMaxHeight(), HARD_MAX_HEIGHT);
                long maxPixels = Math.min(properties.getMaxPixels(), HARD_MAX_PIXELS);
                if (maxWidth <= 0 || maxHeight <= 0 || maxPixels <= 0)
                    throw new ServiceException("头像图片尺寸配置必须大于0");
                if (width <= 0 || height <= 0 || width > maxWidth
                        || height > maxHeight || pixels > maxPixels)
                    throw new ServiceException("头像图片尺寸超出限制");
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw new ServiceException("头像文件内容不是有效图片");
            }
            catch (ServiceException exception) { throw exception; }
            catch (RuntimeException | IOException exception)
            {
                throw new ServiceException("头像文件内容不是有效图片");
            }
            finally { reader.dispose(); }
        }
    }

    private String extension(String filename)
    {
        if (isBlank(filename) || filename.lastIndexOf('.') < 0) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeFormat(String format)
    {
        String value = format.toLowerCase(Locale.ROOT);
        return "jpg".equals(value) ? "jpeg" : value;
    }

    private String trim(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }

    private void ensureConfigured()
    {
        if (root == null) throw new ServiceException("头像存储根目录尚未配置");
    }

    private long effectiveMaxBytes()
    {
        if (properties.getMaxBytes() <= 0) throw new ServiceException("头像文件大小配置必须大于0");
        return Math.min(properties.getMaxBytes(), HARD_MAX_BYTES);
    }

    private void rejectSymbolicLink(Path directory)
    {
        if (Files.isSymbolicLink(directory)) throw new ServiceException("头像存储目录不合法");
    }

    private void rejectSymbolicLinkComponents(Path path)
    {
        Path current = root;
        for (Path component : root.relativize(path))
        {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) throw new ServiceException("头像路径不合法");
        }
    }

    private ServiceException fileTooLarge(long maxBytes)
    {
        return maxBytes == HARD_MAX_BYTES ? new ServiceException("头像文件不能超过2MB")
                : new ServiceException("头像文件超过配置的大小限制");
    }
}
