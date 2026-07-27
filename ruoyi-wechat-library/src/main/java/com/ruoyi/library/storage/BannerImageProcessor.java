package com.ruoyi.library.storage;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Stream;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.DocumentConversionProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** 安全校验并标准化首页轮播图。 */
@Component
public class BannerImageProcessor
{
    public static final int WIDTH = 1120;
    public static final int HEIGHT = 550;
    public static final long MAX_UPLOAD_BYTES = 5L * 1024L * 1024L;
    public static final long MAX_PIXELS = (long) WIDTH * HEIGHT;

    private static final String CONTAINER_NAME = "banner-images";
    private static final String SESSION_PREFIX = "wl-banner-";
    private static final String SESSION_PATTERN = "wl-banner-[0-9a-f]{32}";
    private static final String INPUT_NAME = "input.jpg";
    private static final String OUTPUT_NAME = "normalized.jpg";
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";
    private static final String UNSAFE_PATH_MESSAGE = "轮播图临时路径不安全";

    private final DocumentConversionProperties properties;

    public BannerImageProcessor(DocumentConversionProperties properties)
    {
        this.properties = properties;
    }

    /** 校验上传声明和真实 JPEG 内容，返回待使用的标准化轮播图。 */
    public ProcessedBannerImage process(MultipartFile image)
    {
        validateDeclaration(image);
        Path container = null;
        Path session = null;
        try
        {
            Path root = resolveConfiguredRoot();
            container = resolveContainer(root);
            session = createSession(container);
            Path input = resolveSessionFile(session, INPUT_NAME);
            Path output = resolveSessionFile(session, OUTPUT_NAME);

            copyBounded(image, root, container, session, input);
            BufferedImage decoded = readValidatedJpeg(root, container, session, input);
            writeNormalizedJpeg(root, container, session, output, decoded);
            requireSafeRegularFile(root, container, session, output);
            long size = Files.size(output);
            return new ProcessedBannerImage(root, container, session, output, size);
        }
        catch (ServiceException exception)
        {
            cleanupFailedSession(container, session);
            throw exception;
        }
        catch (IOException | RuntimeException exception)
        {
            cleanupFailedSession(container, session);
            throw new ServiceException("轮播图处理失败，请稍后重试");
        }
    }

    private void validateDeclaration(MultipartFile image)
    {
        if (image == null || image.isEmpty()) throw new ServiceException("请选择轮播图");
        if (image.getSize() > MAX_UPLOAD_BYTES) throw new ServiceException("轮播图不能超过5MB");
        String filename = image.getOriginalFilename();
        int dot = filename == null ? -1 : filename.lastIndexOf('.');
        String extension = dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
        if (!"jpg".equals(extension) && !"jpeg".equals(extension))
            throw new ServiceException("轮播图仅支持JPG格式");
        String contentType = image.getContentType();
        if (contentType == null || !JPEG_CONTENT_TYPE.equalsIgnoreCase(contentType.trim()))
            throw new ServiceException("轮播图MIME类型必须为image/jpeg");
    }

    private Path resolveConfiguredRoot() throws IOException
    {
        String configured = properties.getTempDirectory();
        Path root = configured == null || configured.trim().isEmpty()
                ? Paths.get(System.getProperty("java.io.tmpdir"), "wechat-library-conversion")
                : Paths.get(configured.trim());
        root = root.toAbsolutePath().normalize();
        requireDirectoryOrCreate(root);
        return root;
    }

    private Path resolveContainer(Path root) throws IOException
    {
        requireSafeDirectory(root);
        Path container = root.resolve(CONTAINER_NAME).toAbsolutePath().normalize();
        if (!container.startsWith(root) || !root.equals(container.getParent())) throw unsafePath();
        requireDirectoryOrCreate(container);
        requireSafeDirectory(root);
        requireSafeDirectory(container);
        return container;
    }

    private Path createSession(Path container) throws IOException
    {
        requireSafeDirectory(container);
        String name = SESSION_PREFIX + UUID.randomUUID().toString().replace("-", "");
        Path session = container.resolve(name).toAbsolutePath().normalize();
        if (!session.startsWith(container) || !container.equals(session.getParent())
                || !name.matches(SESSION_PATTERN) || Files.exists(session, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(session))
            throw unsafePath();
        Files.createDirectory(session);
        requireSafeSession(container, session);
        return session;
    }

    private Path resolveSessionFile(Path session, String name)
    {
        Path file = session.resolve(name).toAbsolutePath().normalize();
        if (!file.startsWith(session) || !session.equals(file.getParent())) throw unsafePath();
        return file;
    }

    private void copyBounded(MultipartFile image, Path root, Path container, Path session, Path input)
            throws IOException
    {
        try (InputStream source = image.getInputStream())
        {
            requireSafeHierarchy(root, container, session);
            requireSafeNewFile(input, session);
            OpenOption[] options = {StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS};
            try (OutputStream target = Files.newOutputStream(input, options))
            {
                byte[] buffer = new byte[8192];
                long total = 0L;
                while (true)
                {
                    int allowed = (int) Math.min(buffer.length, MAX_UPLOAD_BYTES + 1L - total);
                    int read = source.read(buffer, 0, allowed);
                    if (read == -1) break;
                    if (read == 0) continue;
                    total += read;
                    if (total > MAX_UPLOAD_BYTES) throw new ServiceException("轮播图不能超过5MB");
                    target.write(buffer, 0, read);
                }
            }
        }
        requireSafeRegularFile(root, container, session, input);
    }

    private BufferedImage readValidatedJpeg(Path root, Path container, Path session, Path input)
            throws IOException
    {
        requireSafeRegularFile(root, container, session, input);
        try (ImageInputStream imageInput = ImageIO.createImageInputStream(input.toFile()))
        {
            if (imageInput == null) throw new ServiceException("轮播图内容无法识别");
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            if (!readers.hasNext()) throw new ServiceException("轮播图内容无法识别");
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(imageInput, true, true);
                String format = reader.getFormatName();
                if (format == null || !"JPEG".equals(normalizeJpegFormat(format)))
                    throw new ServiceException("轮播图文件扩展名与实际格式不一致");
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0) throw new ServiceException("轮播图内容无法识别");
                if ((long) width > MAX_PIXELS / (long) height)
                    throw new ServiceException("轮播图像素数量超出安全限制");
                if (width != WIDTH || height != HEIGHT)
                    throw new ServiceException("轮播图尺寸必须为1120×550");
                BufferedImage decoded = reader.read(0);
                if (decoded == null || decoded.getWidth() != WIDTH || decoded.getHeight() != HEIGHT)
                    throw new ServiceException("轮播图内容无法识别");
                return decoded;
            }
            catch (ServiceException exception) { throw exception; }
            catch (IOException | RuntimeException exception)
            {
                throw new ServiceException("轮播图内容无法识别");
            }
            finally { reader.dispose(); }
        }
    }

    private String normalizeJpegFormat(String format)
    {
        String normalized = format.trim().toUpperCase(Locale.ROOT);
        return "JPG".equals(normalized) ? "JPEG" : normalized;
    }

    private void writeNormalizedJpeg(Path root, Path container, Path session, Path output,
            BufferedImage decoded) throws IOException
    {
        requireSafeHierarchy(root, container, session);
        requireSafeNewFile(output, session);
        OpenOption[] options = {StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE, LinkOption.NOFOLLOW_LINKS};
        try (OutputStream stream = Files.newOutputStream(output, options))
        {
            if (!ImageIO.write(decoded, "jpg", stream))
                throw new ServiceException("轮播图标准化失败，请稍后重试");
        }
    }

    private void requireDirectoryOrCreate(Path directory) throws IOException
    {
        if (Files.exists(directory, LinkOption.NOFOLLOW_LINKS))
        {
            requireSafeDirectory(directory);
            return;
        }
        Files.createDirectories(directory);
        requireSafeDirectory(directory);
    }

    private static void requireSafeHierarchy(Path root, Path container, Path session)
    {
        requireSafeDirectory(root);
        requireSafeDirectory(container);
        requireSafeSession(container, session);
    }

    private static void requireSafeDirectory(Path directory)
    {
        if (directory == null || !directory.isAbsolute() || !directory.equals(directory.normalize())
                || Files.isSymbolicLink(directory)
                || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS))
            throw unsafePath();
    }

    private static void requireSafeSession(Path container, Path session)
    {
        if (session == null || session.getFileName() == null || !session.isAbsolute()
                || !session.equals(session.normalize()) || !session.startsWith(container)
                || !container.equals(session.getParent())
                || !session.getFileName().toString().matches(SESSION_PATTERN)
                || Files.isSymbolicLink(session)
                || !Files.isDirectory(session, LinkOption.NOFOLLOW_LINKS))
            throw unsafePath();
    }

    private static void requireSafeNewFile(Path file, Path session)
    {
        if (file == null || !file.isAbsolute() || !file.equals(file.normalize())
                || !session.equals(file.getParent()) || Files.exists(file, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(file))
            throw unsafePath();
    }

    private static void requireSafeRegularFile(Path root, Path container, Path session, Path file)
    {
        requireSafeHierarchy(root, container, session);
        if (file == null || !file.isAbsolute() || !file.equals(file.normalize())
                || !session.equals(file.getParent()) || Files.isSymbolicLink(file)
                || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS))
            throw unsafePath();
    }

    private static ServiceException unsafePath()
    {
        return new ServiceException(UNSAFE_PATH_MESSAGE);
    }

    private static void cleanupFailedSession(Path container, Path session)
    {
        if (container == null || session == null) return;
        try { deleteVerifiedSession(container, session); }
        catch (RuntimeException | IOException ignored) { /* 安全校验失败时保留现场，禁止递归到未知路径。 */ }
    }

    private static boolean deleteVerifiedSession(Path container, Path session) throws IOException
    {
        requireSafeDirectory(container);
        if (!Files.exists(session, LinkOption.NOFOLLOW_LINKS)) return true;
        requireSafeSession(container, session);
        List<Path> candidates = new ArrayList<>();
        try (Stream<Path> paths = Files.walk(session))
        {
            paths.forEach(candidates::add);
        }
        for (Path candidate : candidates)
        {
            Path normalized = candidate.toAbsolutePath().normalize();
            if (!normalized.startsWith(session) || Files.isSymbolicLink(normalized)) throw unsafePath();
        }
        candidates.sort(Comparator.reverseOrder());
        for (Path candidate : candidates) Files.deleteIfExists(candidate);
        return true;
    }

    /** 标准化后的 JPEG 资源；使用完成后必须关闭以清理临时会话。 */
    public static final class ProcessedBannerImage implements AutoCloseable
    {
        private final Path root;
        private final Path container;
        private final Path session;
        private final Path file;
        private final long size;
        private final AtomicBoolean closed = new AtomicBoolean();

        private ProcessedBannerImage(Path root, Path container, Path session, Path file, long size)
        {
            this.root = root;
            this.container = container;
            this.session = session;
            this.file = file;
            this.size = size;
        }

        /** 打开标准化 JPEG 输入流，调用方负责关闭该流。 */
        public InputStream openStream()
        {
            if (closed.get()) throw new ServiceException("轮播图临时文件已清理");
            try
            {
                requireSafeRegularFile(root, container, session, file);
                return Files.newInputStream(file, StandardOpenOption.READ, LinkOption.NOFOLLOW_LINKS);
            }
            catch (ServiceException exception) { throw exception; }
            catch (IOException exception) { throw new ServiceException("轮播图临时文件读取失败"); }
        }

        public long getSize()
        {
            return size;
        }

        public String getContentType()
        {
            return JPEG_CONTENT_TYPE;
        }

        /** 仅在会话目录通过边界和符号链接校验后递归清理。 */
        @Override
        public void close()
        {
            if (!closed.compareAndSet(false, true)) return;
            try
            {
                if (!deleteVerifiedSession(container, session)) throw unsafePath();
            }
            catch (ServiceException exception) { throw exception; }
            catch (IOException exception) { throw new ServiceException("轮播图临时文件清理失败"); }
        }
    }
}
