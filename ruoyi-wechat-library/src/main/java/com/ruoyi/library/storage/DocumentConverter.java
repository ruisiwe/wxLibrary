package com.ruoyi.library.storage;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.stream.Stream;

/** 文档转换受限进程端口。 */
public interface DocumentConverter
{
    ConversionArtifacts convert(URL sourceUrl, String extension, int previewPages, long maxOutputBytes);

    /** 转换产生的临时完整 PDF、试读 PDF 和页数。 */
    final class ConversionArtifacts implements AutoCloseable
    {
        private final Path fullPdf;
        private final Path previewPdf;
        private final int pageCount;

        public ConversionArtifacts(Path fullPdf, Path previewPdf, int pageCount)
        {
            this.fullPdf = fullPdf;
            this.previewPdf = previewPdf;
            this.pageCount = pageCount;
        }

        public Path getFullPdf() { return fullPdf; }
        public Path getPreviewPdf() { return previewPdf; }
        public int getPageCount() { return pageCount; }

        @Override
        public void close()
        {
            Path parent = fullPdf == null ? null : fullPdf.getParent();
            if (parent != null && parent.getFileName() != null
                    && parent.getFileName().toString().startsWith("wl-convert-")
                    && previewPdf != null && parent.equals(previewPdf.getParent()))
            {
                deleteTree(parent);
                return;
            }
            delete(fullPdf);
            delete(previewPdf);
        }

        private void delete(Path path)
        {
            if (path == null) return;
            try { Files.deleteIfExists(path); }
            catch (IOException ignored) { }
        }

        private void deleteTree(Path directory)
        {
            try (Stream<Path> paths = Files.walk(directory))
            {
                paths.sorted(Comparator.reverseOrder()).forEach(this::delete);
            }
            catch (IOException ignored) { }
        }
    }
}
