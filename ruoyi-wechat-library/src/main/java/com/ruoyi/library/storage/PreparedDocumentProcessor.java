package com.ruoyi.library.storage;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.DocumentConversionProperties;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.rendering.ImageType;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Component;

/** 将本地原文件转换为固定页数的试看 PDF 和首页 JPG。 */
@Component
public class PreparedDocumentProcessor
{
    private static final long MAX_THUMBNAIL_PIXELS = 20_000_000L;
    private static final int THUMBNAIL_LONGEST_SIDE = 800;
    private final DocumentConversionProperties properties;

    public PreparedDocumentProcessor(DocumentConversionProperties properties)
    {
        this.properties = properties;
    }

    public PreparedDocument prepare(Path original, String extension, Path sessionDirectory, long maxOutputBytes)
    {
        String safeExtension = normalizeExtension(extension);
        requireSafeFile(original, sessionDirectory, "上传临时文件不安全");
        requireSafeDirectory(sessionDirectory);
        Path full = sessionDirectory.resolve("full.pdf");
        Path preview = sessionDirectory.resolve("preview.pdf");
        Path thumbnail = sessionDirectory.resolve("thumbnail.jpg");
        try
        {
            if ("PDF".equals(safeExtension)) Files.copy(original, full, StandardCopyOption.REPLACE_EXISTING);
            else convertWithLibreOffice(original, full, sessionDirectory);
            ensureOutput(full, maxOutputBytes);
            int pageCount;
            int previewPages;
            try (PDDocument source = Loader.loadPDF(full.toFile()))
            {
                pageCount = source.getNumberOfPages();
                if (pageCount < 2) throw new ServiceException("文档总页数不能少于2页");
                previewPages = pageCount == 2 ? 1 : 2;
                createPreview(source, preview, previewPages);
                createThumbnail(source, thumbnail);
            }
            ensureOutput(preview, maxOutputBytes);
            ensureOutput(thumbnail, Math.min(maxOutputBytes, 5L * 1024L * 1024L));
            return new PreparedDocument(preview, thumbnail, pageCount, previewPages);
        }
        catch (ServiceException exception)
        {
            deleteQuietly(preview);
            deleteQuietly(thumbnail);
            throw exception;
        }
        catch (Exception exception)
        {
            deleteQuietly(preview);
            deleteQuietly(thumbnail);
            throw new ServiceException("文件处理失败，请重试");
        }
        finally
        {
            deleteQuietly(full);
        }
    }

    private void convertWithLibreOffice(Path original, Path full, Path sessionDirectory) throws Exception
    {
        String configured = properties.getExecutable();
        if (configured == null || configured.trim().isEmpty()) throw new ServiceException("文档转换程序未配置");
        Path executable = Paths.get(configured.trim()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(executable, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(executable))
            throw new ServiceException("文档转换程序路径不正确");
        Path profile = sessionDirectory.resolve("profile");
        Files.createDirectory(profile);
        ProcessBuilder builder = new ProcessBuilder(Arrays.asList(
                executable.toString(), "-env:UserInstallation=" + profile.toUri(),
                "--headless", "--nologo", "--nodefault", "--nolockcheck", "--norestore",
                "--convert-to", "pdf", "--outdir", sessionDirectory.toString(), original.toString()));
        builder.directory(sessionDirectory.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(sessionDirectory.resolve("converter.log").toFile());
        Process process = builder.start();
        long timeout = properties.getTimeoutSeconds();
        if (timeout < 1 || !process.waitFor(timeout, TimeUnit.SECONDS))
        {
            process.destroyForcibly();
            throw new ServiceException("文件处理超时，请重试");
        }
        if (process.exitValue() != 0) throw new ServiceException("文件处理失败，请检查文件内容");
        String name = original.getFileName().toString();
        int separator = name.lastIndexOf('.');
        Path generated = sessionDirectory.resolve((separator > 0 ? name.substring(0, separator) : name) + ".pdf");
        if (!Files.isRegularFile(generated, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(generated))
            throw new ServiceException("文件处理未生成 PDF");
        Files.move(generated, full, StandardCopyOption.REPLACE_EXISTING);
    }

    private void createPreview(PDDocument source, Path preview, int pages) throws IOException
    {
        try (PDDocument target = new PDDocument())
        {
            for (int index = 0; index < pages; index++) target.importPage(source.getPage(index));
            target.save(preview.toFile());
        }
    }

    private void createThumbnail(PDDocument source, Path thumbnail) throws IOException
    {
        PDPage first = source.getPage(0);
        float width = Math.max(first.getMediaBox().getWidth(), 1F);
        float height = Math.max(first.getMediaBox().getHeight(), 1F);
        float scale = Math.min(1.5F, THUMBNAIL_LONGEST_SIDE / Math.max(width, height));
        scale = Math.max(scale, 0.05F);
        BufferedImage rendered = new PDFRenderer(source).renderImage(0, scale, ImageType.RGB);
        if ((long) rendered.getWidth() * rendered.getHeight() > MAX_THUMBNAIL_PIXELS)
            throw new ServiceException("文档首页尺寸过大");
        BufferedImage jpeg = new BufferedImage(rendered.getWidth(), rendered.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = jpeg.createGraphics();
        try { graphics.drawImage(rendered, 0, 0, null); }
        finally { graphics.dispose(); }
        if (!ImageIO.write(jpeg, "jpg", thumbnail.toFile())) throw new ServiceException("缩略图生成失败");
    }

    private void requireSafeDirectory(Path directory)
    {
        if (directory == null || !Files.isDirectory(directory, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(directory)) throw new ServiceException("文档临时目录不安全");
    }

    private void requireSafeFile(Path file, Path directory, String message)
    {
        if (file == null || directory == null || !file.toAbsolutePath().normalize().getParent().equals(
                directory.toAbsolutePath().normalize()) || !Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)
                || Files.isSymbolicLink(file)) throw new ServiceException(message);
    }

    private void ensureOutput(Path path, long maxBytes) throws IOException
    {
        if (!Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(path)
                || Files.size(path) < 1) throw new ServiceException("文件处理结果为空");
        if (maxBytes < 1 || Files.size(path) > maxBytes) throw new ServiceException("文件处理结果超过大小限制");
    }

    private String normalizeExtension(String value)
    {
        if (value == null) throw new ServiceException("文档格式不能为空");
        String extension = value.trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("PDF", "DOC", "DOCX", "PPT", "PPTX", "XLS", "XLSX", "TXT").contains(extension))
            throw new ServiceException("文件格式不支持");
        return extension;
    }

    private void deleteQuietly(Path path)
    {
        if (path == null) return;
        try { Files.deleteIfExists(path); }
        catch (IOException ignored) { }
    }

    /** 预处理后保留在会话目录中的文件。 */
    public static final class PreparedDocument
    {
        private final Path previewPdf;
        private final Path thumbnail;
        private final int pageCount;
        private final int previewPages;

        PreparedDocument(Path previewPdf, Path thumbnail, int pageCount, int previewPages)
        {
            this.previewPdf = previewPdf;
            this.thumbnail = thumbnail;
            this.pageCount = pageCount;
            this.previewPages = previewPages;
        }

        public Path getPreviewPdf() { return previewPdf; }
        public Path getThumbnail() { return thumbnail; }
        public int getPageCount() { return pageCount; }
        public int getPreviewPages() { return previewPages; }
    }
}
