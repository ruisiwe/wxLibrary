package com.ruoyi.library.storage;

import com.ruoyi.common.exception.ServiceException;
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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/** 通用二维码与 VIP 客服二维码的本地安全存储服务。 */
@Service
public class QrImageStorageService
{
    private static final long MAX_BYTES = 2L * 1024 * 1024;
    private static final Map<String, String> MIME_BY_EXTENSION = new HashMap<>();

    static
    {
        MIME_BY_EXTENSION.put("jpg", "image/jpeg");
        MIME_BY_EXTENSION.put("jpeg", "image/jpeg");
        MIME_BY_EXTENSION.put("png", "image/png");
        MIME_BY_EXTENSION.put("webp", "image/webp");
    }

    private final Path qrConfigRoot;
    private final Path vipCustomerServiceRoot;

    public QrImageStorageService(WechatProfileStoragePaths paths)
    {
        this.qrConfigRoot = paths.qrConfigRoot();
        this.vipCustomerServiceRoot = paths.vipCustomerServiceRoot();
    }

    public String storeQrConfig(MultipartFile file)
    {
        return store(file, qrConfigRoot);
    }

    public String storeVipCustomerService(MultipartFile file)
    {
        return store(file, vipCustomerServiceRoot);
    }

    public Path resolveQrConfigForRead(String relativePath)
    {
        return resolveForRead(qrConfigRoot, relativePath);
    }

    public Path resolveVipCustomerServiceForRead(String relativePath)
    {
        return resolveForRead(vipCustomerServiceRoot, relativePath);
    }

    public void deleteQrConfigQuietly(String relativePath)
    {
        deleteQuietly(qrConfigRoot, relativePath);
    }

    public void deleteVipCustomerServiceQuietly(String relativePath)
    {
        deleteQuietly(vipCustomerServiceRoot, relativePath);
    }

    private String store(MultipartFile file, Path root)
    {
        if (file == null || file.isEmpty()) throw new ServiceException("请选择有效的二维码图片");
        if (file.getSize() > MAX_BYTES) throw new ServiceException("二维码图片不能超过2MB");
        String extension = extension(file.getOriginalFilename());
        String expectedMime = MIME_BY_EXTENSION.get(extension);
        if (expectedMime == null) throw new ServiceException("二维码图片仅支持JPEG、PNG或WebP格式");
        if (!expectedMime.equalsIgnoreCase(trim(file.getContentType())))
            throw new ServiceException("二维码图片扩展名与类型不匹配");

        Path temp = null;
        try
        {
            rejectSymbolicLink(root);
            Files.createDirectories(root);
            rejectSymbolicLink(root);
            temp = Files.createTempFile(root, ".qr-", ".tmp");
            file.transferTo(temp.toFile());
            if (Files.size(temp) > MAX_BYTES) throw new ServiceException("二维码图片不能超过2MB");
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
            throw new ServiceException("二维码图片保存失败，请稍后重试");
        }
        finally
        {
            if (temp != null)
            {
                try { Files.deleteIfExists(temp); }
                catch (IOException ignored) { /* 临时文件由系统后续回收。 */ }
            }
        }
    }

    private Path resolveForRead(Path root, String relativePath)
    {
        if (isBlank(relativePath)) throw new ServiceException("二维码图片路径不合法");
        Path relative;
        try { relative = Paths.get(relativePath); }
        catch (RuntimeException exception) { throw new ServiceException("二维码图片路径不合法"); }
        if (relative.isAbsolute()) throw new ServiceException("二维码图片路径不合法");
        Path resolved = root.resolve(relative).normalize();
        if (!resolved.startsWith(root)) throw new ServiceException("二维码图片路径不合法");
        rejectSymbolicLink(root);
        rejectSymbolicLinkComponents(root, resolved);
        if (!Files.isRegularFile(resolved, LinkOption.NOFOLLOW_LINKS))
            throw new ServiceException("二维码图片不存在");
        return resolved;
    }

    private void deleteQuietly(Path root, String relativePath)
    {
        try { Files.deleteIfExists(resolveForRead(root, relativePath)); }
        catch (RuntimeException | IOException ignored) { /* 清理失败不覆盖主业务结果。 */ }
    }

    private void validateImage(Path file, String expectedExtension) throws IOException
    {
        try (ImageInputStream input = ImageIO.createImageInputStream(file.toFile()))
        {
            if (input == null) throw new ServiceException("二维码图片内容无效");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw new ServiceException("二维码图片内容无效");
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(input, true, true);
                String actual = normalizeFormat(reader.getFormatName());
                String expected = normalizeFormat(expectedExtension);
                if (!expected.equals(actual)) throw new ServiceException("二维码图片扩展名与实际内容不匹配");
                if (reader.getWidth(0) <= 0 || reader.getHeight(0) <= 0)
                    throw new ServiceException("二维码图片内容无效");
                BufferedImage decoded = reader.read(0);
                if (decoded == null) throw new ServiceException("二维码图片内容无效");
            }
            catch (ServiceException exception)
            {
                throw exception;
            }
            catch (RuntimeException | IOException exception)
            {
                throw new ServiceException("二维码图片内容无效");
            }
            finally
            {
                reader.dispose();
            }
        }
    }

    private void rejectSymbolicLink(Path directory)
    {
        if (Files.isSymbolicLink(directory)) throw new ServiceException("二维码存储目录不合法");
    }

    private void rejectSymbolicLinkComponents(Path root, Path path)
    {
        Path current = root;
        for (Path component : root.relativize(path))
        {
            current = current.resolve(component);
            if (Files.isSymbolicLink(current)) throw new ServiceException("二维码图片路径不合法");
        }
    }

    private String extension(String filename)
    {
        if (isBlank(filename) || filename.lastIndexOf('.') < 0) return "";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private String normalizeFormat(String value)
    {
        String normalized = value.toLowerCase(Locale.ROOT);
        return "jpg".equals(normalized) ? "jpeg" : normalized;
    }

    private String trim(String value)
    {
        return value == null ? null : value.trim();
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
