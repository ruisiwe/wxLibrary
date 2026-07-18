package com.ruoyi.library.storage;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.DocumentConversionProperties;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import java.util.stream.Stream;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.springframework.stereotype.Component;

/** 不经过 shell、带超时和大小限制的 LibreOffice 文档转换适配器。 */
@Component
public class RestrictedProcessDocumentConverter implements DocumentConverter
{
    private final DocumentConversionProperties properties;

    public RestrictedProcessDocumentConverter(DocumentConversionProperties properties)
    {
        this.properties = properties;
    }

    @Override
    public ConversionArtifacts convert(URL sourceUrl, String extension, int previewPages, long maxOutputBytes)
    {
        if (sourceUrl == null) throw new ServiceException("原文件临时地址不能为空");
        String safeExtension = normalizeExtension(extension);
        Path work = createWorkDirectory();
        try
        {
            Path source = work.resolve("source." + safeExtension.toLowerCase(Locale.ROOT));
            download(sourceUrl, source, properties.getMaxInputBytes());
            Path full = work.resolve("full.pdf");
            if ("PDF".equals(safeExtension)) Files.copy(source, full, StandardCopyOption.REPLACE_EXISTING);
            else runOfficeConversion(source, full, work);
            ensureSize(full, Math.min(maxOutputBytes, properties.getMaxOutputBytes()));
            Path preview = work.resolve("preview.pdf");
            int pageCount = createPreview(full, preview, previewPages);
            ensureSize(preview, Math.min(maxOutputBytes, properties.getMaxOutputBytes()));
            return new ConversionArtifacts(full, preview, pageCount);
        }
        catch (ServiceException exception)
        {
            deleteWorkDirectory(work);
            throw exception;
        }
        catch (Exception exception)
        {
            deleteWorkDirectory(work);
            throw new ServiceException("文档转换失败，请检查文件内容");
        }
    }

    private Path createWorkDirectory()
    {
        try
        {
            Path root = properties.getTempDirectory() == null || properties.getTempDirectory().trim().isEmpty()
                    ? Paths.get(System.getProperty("java.io.tmpdir"), "wechat-library-conversion")
                    : Paths.get(properties.getTempDirectory().trim());
            root = root.toAbsolutePath().normalize();
            Files.createDirectories(root);
            if (Files.isSymbolicLink(root)) throw new ServiceException("文档转换临时目录不安全");
            return Files.createTempDirectory(root, "wl-convert-");
        }
        catch (IOException exception)
        {
            throw new ServiceException("无法创建文档转换临时目录");
        }
    }

    private void download(URL sourceUrl, Path target, long maxBytes) throws IOException
    {
        if (maxBytes < 1) throw new ServiceException("文档转换输入大小限制不正确");
        URLConnection connection = sourceUrl.openConnection();
        connection.setConnectTimeout(10_000);
        connection.setReadTimeout(30_000);
        long declared = connection.getContentLengthLong();
        if (declared > maxBytes) throw new ServiceException("原文件超过文档转换大小限制");
        try (InputStream input = connection.getInputStream(); java.io.OutputStream output = Files.newOutputStream(target))
        {
            byte[] buffer = new byte[8192];
            long total = 0L;
            int read;
            while ((read = input.read(buffer)) != -1)
            {
                total += read;
                if (total > maxBytes) throw new ServiceException("原文件超过文档转换大小限制");
                output.write(buffer, 0, read);
            }
        }
    }

    private void runOfficeConversion(Path source, Path full, Path work) throws Exception
    {
        String configured = properties.getExecutable();
        if (configured == null || configured.trim().isEmpty()) throw new ServiceException("文档转换程序未配置");
        Path executable = Paths.get(configured.trim()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(executable, LinkOption.NOFOLLOW_LINKS) || Files.isSymbolicLink(executable))
            throw new ServiceException("文档转换程序路径不正确");
        Path profile = work.resolve("profile");
        Files.createDirectory(profile);
        ProcessBuilder builder = new ProcessBuilder(Arrays.asList(
                executable.toString(), "-env:UserInstallation=" + profile.toUri(),
                "--headless", "--nologo", "--nodefault", "--nolockcheck", "--norestore",
                "--convert-to", "pdf", "--outdir", work.toString(), source.toString()));
        builder.directory(work.toFile());
        builder.redirectErrorStream(true);
        builder.redirectOutput(work.resolve("converter.log").toFile());
        Process process = builder.start();
        long timeout = properties.getTimeoutSeconds();
        if (timeout < 1 || !process.waitFor(timeout, TimeUnit.SECONDS))
        {
            process.destroyForcibly();
            throw new ServiceException("转换进程超时");
        }
        if (process.exitValue() != 0) throw new ServiceException("转换进程执行失败");
        Path generated = work.resolve("source.pdf");
        if (!Files.isRegularFile(generated)) throw new ServiceException("转换进程未生成 PDF 文件");
        Files.move(generated, full, StandardCopyOption.REPLACE_EXISTING);
    }

    private int createPreview(Path full, Path preview, int previewPages) throws IOException
    {
        try (PDDocument source = Loader.loadPDF(full.toFile()))
        {
            int pageCount = source.getNumberOfPages();
            if (previewPages <= 0 || previewPages >= pageCount)
                throw new ServiceException("试读页数必须大于0且小于文档总页数");
            try (PDDocument target = new PDDocument())
            {
                for (int index = 0; index < previewPages; index++) target.importPage(source.getPage(index));
                target.save(preview.toFile());
            }
            return pageCount;
        }
    }

    private void ensureSize(Path path, long maxBytes) throws IOException
    {
        if (!Files.isRegularFile(path) || Files.size(path) < 1) throw new ServiceException("转换结果文件为空");
        if (maxBytes < 1 || Files.size(path) > maxBytes) throw new ServiceException("转换结果超过大小限制");
    }

    private String normalizeExtension(String value)
    {
        if (value == null) throw new ServiceException("文档格式不能为空");
        String extension = value.trim().toUpperCase(Locale.ROOT);
        if (!Arrays.asList("PDF", "DOC", "DOCX", "PPT", "PPTX", "TXT", "XLS").contains(extension))
            throw new ServiceException("文档格式不支持");
        return extension;
    }

    private void deleteWorkDirectory(Path work)
    {
        if (work == null || work.getFileName() == null || !work.getFileName().toString().startsWith("wl-convert-")) return;
        try
        {
            try (Stream<Path> paths = Files.walk(work))
            {
                paths.sorted(java.util.Comparator.reverseOrder()).forEach(path -> {
                    try { Files.deleteIfExists(path); }
                    catch (IOException ignored) { }
                });
            }
        }
        catch (IOException ignored) { }
    }
}
