package com.ruoyi.library.storage;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.DocumentConversionProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BannerImageProcessorTest
{
    @TempDir
    Path root;

    private BannerImageProcessor processor;

    @BeforeEach
    void setUp()
    {
        processor = processor(root);
    }

    @Test
    void normalizesRealJpegAndDeletesSessionAfterClose() throws Exception
    {
        BannerImageProcessor.ProcessedBannerImage processed = processor.process(
                file("banner.jpg", "image/jpeg", jpeg(BannerImageProcessor.WIDTH, BannerImageProcessor.HEIGHT)));
        Path container = root.resolve("banner-images");
        Path session = onlySession(container);

        assertTrue(session.getFileName().toString().matches("wl-banner-[0-9a-f]{32}"));
        assertEquals("image/jpeg", processed.getContentType());
        assertTrue(processed.getSize() > 0L);
        try (InputStream stream = processed.openStream(); ImageInputStream imageInput = ImageIO.createImageInputStream(stream))
        {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInput);
            assertTrue(readers.hasNext());
            ImageReader reader = readers.next();
            try
            {
                reader.setInput(imageInput, true, true);
                assertEquals("JPEG", reader.getFormatName().toUpperCase());
                assertEquals(BannerImageProcessor.WIDTH, reader.getWidth(0));
                assertEquals(BannerImageProcessor.HEIGHT, reader.getHeight(0));
            }
            finally { reader.dispose(); }
        }

        processed.close();

        assertFalse(Files.exists(session));
        assertThrows(ServiceException.class, processed::openStream);
    }

    @Test
    void rejectsWrongDimensions() throws Exception
    {
        ServiceException exception = assertThrows(ServiceException.class, () -> processor.process(
                file("banner.jpg", "image/jpeg", jpeg(BannerImageProcessor.WIDTH, 479))));

        assertEquals("轮播图尺寸必须为952×550", exception.getMessage());
        assertContainerHasNoSessions(root.resolve("banner-images"));
    }

    @Test
    void rejectsPngRenamedAsJpg() throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(BannerImageProcessor.WIDTH, BannerImageProcessor.HEIGHT,
                BufferedImage.TYPE_INT_RGB), "png", output);

        ServiceException exception = assertThrows(ServiceException.class, () -> processor.process(
                file("banner.jpg", "image/jpeg", output.toByteArray())));

        assertEquals("轮播图文件扩展名与实际格式不一致", exception.getMessage());
    }

    @Test
    void rejectsEmptyFile()
    {
        ServiceException exception = assertThrows(ServiceException.class, () -> processor.process(
                file("banner.jpg", "image/jpeg", new byte[0])));

        assertEquals("请选择轮播图", exception.getMessage());
    }

    @Test
    void rejectsNonJpegExtension() throws Exception
    {
        ServiceException exception = assertThrows(ServiceException.class, () -> processor.process(
                file("banner.png", "image/jpeg", jpeg(BannerImageProcessor.WIDTH, BannerImageProcessor.HEIGHT))));

        assertEquals("轮播图仅支持JPG格式", exception.getMessage());
    }

    @Test
    void rejectsNonJpegMimeType() throws Exception
    {
        ServiceException exception = assertThrows(ServiceException.class, () -> processor.process(
                file("banner.jpeg", "image/png", jpeg(BannerImageProcessor.WIDTH, BannerImageProcessor.HEIGHT))));

        assertEquals("轮播图MIME类型必须为image/jpeg", exception.getMessage());
    }

    @Test
    void stopsReadingAtFiveMibPlusOneEvenWhenReportedSizeIsSmall() throws Exception
    {
        MultipartFile misleading = mock(MultipartFile.class);
        when(misleading.isEmpty()).thenReturn(false);
        when(misleading.getSize()).thenReturn(1L);
        when(misleading.getOriginalFilename()).thenReturn("banner.jpg");
        when(misleading.getContentType()).thenReturn("image/jpeg");
        CountingLimitInputStream input = new CountingLimitInputStream(BannerImageProcessor.MAX_UPLOAD_BYTES + 1L);
        when(misleading.getInputStream()).thenReturn(input);

        ServiceException exception = assertThrows(ServiceException.class, () -> processor.process(misleading));

        assertEquals("轮播图不能超过5MB", exception.getMessage());
        assertEquals(BannerImageProcessor.MAX_UPLOAD_BYTES + 1L, input.getReadCount());
    }

    @Test
    void rejectsPixelCountBeforeDecoding() throws Exception
    {
        byte[] oversizedMetadata = jpeg(BannerImageProcessor.WIDTH, BannerImageProcessor.HEIGHT + 1);

        ServiceException exception = assertThrows(ServiceException.class, () -> processor.process(
                file("banner.jpg", "image/jpeg", oversizedMetadata)));

        assertEquals("轮播图像素数量超出安全限制", exception.getMessage());
    }

    @Test
    void rejectsSymbolicLinkConfiguredRootWhenSupported() throws Exception
    {
        Path target = Files.createDirectory(root.resolve("root-target"));
        Path link = root.resolve("root-link");
        createSymbolicLinkOrSkip(link, target);

        ServiceException exception = assertThrows(ServiceException.class, () -> processor(link).process(validFile()));

        assertEquals("轮播图临时路径不安全", exception.getMessage());
    }

    @Test
    void rejectsSymbolicLinkContainerWhenSupported() throws Exception
    {
        Path target = Files.createDirectory(root.resolve("container-target"));
        createSymbolicLinkOrSkip(root.resolve("banner-images"), target);

        ServiceException exception = assertThrows(ServiceException.class, () -> processor.process(validFile()));

        assertEquals("轮播图临时路径不安全", exception.getMessage());
    }

    @Test
    void rejectsSymbolicLinkSessionWhenSupported() throws Exception
    {
        assumeSymbolicLinksSupported();
        Path outside = Files.createDirectory(root.resolve("outside-session"));
        Path marker = Files.write(outside.resolve("marker.txt"), new byte[] {1});
        MultipartFile malicious = multipartWithStreamAction(() -> {
            Path session = onlySession(root.resolve("banner-images"));
            Files.delete(session);
            Files.createSymbolicLink(session, outside);
        }, jpeg(BannerImageProcessor.WIDTH, BannerImageProcessor.HEIGHT), false);

        ServiceException exception = assertThrows(ServiceException.class, () -> processor.process(malicious));

        assertEquals("轮播图临时路径不安全", exception.getMessage());
        assertTrue(Files.exists(marker));
    }

    @Test
    void rejectsSymbolicLinkInputWhenSupported() throws Exception
    {
        assumeSymbolicLinksSupported();
        Path outside = Files.write(root.resolve("outside-input.jpg"), new byte[] {9});
        MultipartFile malicious = multipartWithStreamAction(() -> {
            Path session = onlySession(root.resolve("banner-images"));
            Files.createSymbolicLink(session.resolve("input.jpg"), outside);
        }, jpeg(BannerImageProcessor.WIDTH, BannerImageProcessor.HEIGHT), false);

        ServiceException exception = assertThrows(ServiceException.class, () -> processor.process(malicious));

        assertEquals("轮播图临时路径不安全", exception.getMessage());
        assertEquals(1L, Files.size(outside));
    }

    @Test
    void rejectsSymbolicLinkNormalizedOutputWhenSupported() throws Exception
    {
        assumeSymbolicLinksSupported();
        Path outside = Files.write(root.resolve("outside-output.jpg"), new byte[] {7});
        MultipartFile malicious = multipartWithStreamAction(() -> {
            Path session = onlySession(root.resolve("banner-images"));
            Files.createSymbolicLink(session.resolve("normalized.jpg"), outside);
        }, jpeg(BannerImageProcessor.WIDTH, BannerImageProcessor.HEIGHT), true);

        ServiceException exception = assertThrows(ServiceException.class, () -> processor.process(malicious));

        assertEquals("轮播图临时路径不安全", exception.getMessage());
        assertEquals(1L, Files.size(outside));
    }

    private BannerImageProcessor processor(Path configuredRoot)
    {
        WechatProfileStoragePaths paths = mock(WechatProfileStoragePaths.class);
        when(paths.documentTempRoot()).thenReturn(configuredRoot);
        return new BannerImageProcessor(paths);
    }

    private MockMultipartFile validFile() throws Exception
    {
        return file("banner.jpg", "image/jpeg", jpeg(BannerImageProcessor.WIDTH, BannerImageProcessor.HEIGHT));
    }

    private MockMultipartFile file(String name, String mime, byte[] content)
    {
        return new MockMultipartFile("image", name, mime, content);
    }

    private byte[] jpeg(int width, int height) throws Exception
    {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        assertTrue(ImageIO.write(new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB), "jpg", output));
        return output.toByteArray();
    }

    private MultipartFile multipartWithStreamAction(IoAction action, byte[] content, boolean atEnd) throws Exception
    {
        MultipartFile file = mock(MultipartFile.class);
        when(file.isEmpty()).thenReturn(false);
        when(file.getSize()).thenReturn((long) content.length);
        when(file.getOriginalFilename()).thenReturn("banner.jpg");
        when(file.getContentType()).thenReturn("image/jpeg");
        when(file.getInputStream()).thenAnswer(invocation -> new ActionInputStream(content, action, atEnd));
        return file;
    }

    private Path onlySession(Path container) throws IOException
    {
        try (java.util.stream.Stream<Path> paths = Files.list(container))
        {
            return paths.filter(path -> path.getFileName().toString().startsWith("wl-banner-"))
                    .findFirst().orElseThrow(() -> new IOException("未找到轮播图会话目录"));
        }
    }

    private void assertContainerHasNoSessions(Path container) throws IOException
    {
        if (!Files.exists(container)) return;
        try (java.util.stream.Stream<Path> paths = Files.list(container))
        {
            assertFalse(paths.anyMatch(path -> path.getFileName().toString().startsWith("wl-banner-")));
        }
    }

    private void createSymbolicLinkOrSkip(Path link, Path target) throws IOException
    {
        try
        {
            Files.createSymbolicLink(link, target);
        }
        catch (UnsupportedOperationException | IOException | SecurityException exception)
        {
            assumeTrue(false, "当前平台不支持创建符号链接");
        }
    }

    private void assumeSymbolicLinksSupported() throws IOException
    {
        Path target = Files.createDirectory(root.resolve("symlink-probe-target"));
        Path link = root.resolve("symlink-probe-link");
        createSymbolicLinkOrSkip(link, target);
        Files.delete(link);
        Files.delete(target);
    }

    private interface IoAction
    {
        void run() throws IOException;
    }

    private static final class ActionInputStream extends ByteArrayInputStream
    {
        private final IoAction action;
        private final boolean atEnd;
        private final AtomicBoolean invoked = new AtomicBoolean();

        private ActionInputStream(byte[] content, IoAction action, boolean atEnd) throws IOException
        {
            super(content);
            this.action = action;
            this.atEnd = atEnd;
            if (!atEnd) invoke();
        }

        @Override
        public synchronized int read(byte[] buffer, int offset, int length)
        {
            int read = super.read(buffer, offset, length);
            if (read == -1 && atEnd)
            {
                try { invoke(); }
                catch (IOException exception) { throw new IllegalStateException(exception); }
            }
            return read;
        }

        private void invoke() throws IOException
        {
            if (invoked.compareAndSet(false, true)) action.run();
        }
    }

    private static final class CountingLimitInputStream extends InputStream
    {
        private final long limit;
        private long readCount;

        private CountingLimitInputStream(long limit)
        {
            this.limit = limit;
        }

        @Override
        public int read(byte[] buffer, int offset, int length)
        {
            if (readCount >= limit) throw new AssertionError("处理器读取了超过5MiB+1的数据");
            int read = (int) Math.min(length, limit - readCount);
            java.util.Arrays.fill(buffer, offset, offset + read, (byte) 0);
            readCount += read;
            return read;
        }

        @Override
        public int read()
        {
            if (readCount >= limit) throw new AssertionError("处理器读取了超过5MiB+1的数据");
            readCount++;
            return 0;
        }

        private long getReadCount()
        {
            return readCount;
        }
    }
}
