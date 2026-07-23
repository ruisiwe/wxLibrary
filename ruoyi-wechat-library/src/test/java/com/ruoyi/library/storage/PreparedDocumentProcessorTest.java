package com.ruoyi.library.storage;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.DocumentConversionProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.imageio.ImageIO;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PreparedDocumentProcessorTest
{
    @TempDir
    Path tempDir;

    @Test
    void scannedPdfImageDecodersAreAvailable()
    {
        assertTrue(ImageIO.getImageReadersByFormatName("JPEG2000").hasNext(),
                "应支持 PDF 内嵌 JPEG2000/JPX 扫描页");
        assertTrue(ImageIO.getImageReadersByFormatName("JBIG2").hasNext(),
                "应支持 PDF 内嵌 JBIG2 扫描页");
    }

    @Test
    void twoPagePdfCreatesOnePagePreviewAndJpegThumbnail() throws Exception
    {
        Path session = Files.createDirectory(tempDir.resolve("session-two"));
        Path source = createPdf(session.resolve("original.pdf"), 2);
        PreparedDocumentProcessor processor = processor();

        PreparedDocumentProcessor.PreparedDocument result = processor.prepare(
                source, "PDF", session, 10L * 1024L * 1024L);

        assertEquals(2, result.getPageCount());
        assertEquals(1, result.getPreviewPages());
        try (PDDocument preview = Loader.loadPDF(result.getPreviewPdf().toFile()))
        {
            assertEquals(1, preview.getNumberOfPages());
        }
        byte[] header = Files.readAllBytes(result.getThumbnail());
        assertTrue(header.length > 2 && (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8);
        assertTrue(Files.notExists(session.resolve("full.pdf")));
    }

    @Test
    void threePagePdfCreatesTwoPagePreview() throws Exception
    {
        Path session = Files.createDirectory(tempDir.resolve("session-three"));
        Path source = createPdf(session.resolve("original.pdf"), 3);

        PreparedDocumentProcessor.PreparedDocument result = processor().prepare(
                source, "PDF", session, 10L * 1024L * 1024L);

        assertEquals(2, result.getPreviewPages());
        try (PDDocument preview = Loader.loadPDF(result.getPreviewPdf().toFile()))
        {
            assertEquals(2, preview.getNumberOfPages());
        }
    }

    @Test
    void onePagePdfIsRejected() throws Exception
    {
        Path session = Files.createDirectory(tempDir.resolve("session-one"));
        Path source = createPdf(session.resolve("original.pdf"), 1);

        ServiceException exception = assertThrows(ServiceException.class, () -> processor().prepare(
                source, "PDF", session, 10L * 1024L * 1024L));

        assertEquals("文档总页数不能少于2页", exception.getMessage());
    }

    private PreparedDocumentProcessor processor()
    {
        DocumentConversionProperties properties = new DocumentConversionProperties();
        properties.setTempDirectory(tempDir.toString());
        properties.setMaxInputBytes(10L * 1024L * 1024L);
        properties.setMaxOutputBytes(10L * 1024L * 1024L);
        return new PreparedDocumentProcessor(properties);
    }

    private Path createPdf(Path path, int pages) throws Exception
    {
        try (PDDocument pdf = new PDDocument())
        {
            for (int index = 0; index < pages; index++) pdf.addPage(new PDPage());
            pdf.save(path.toFile());
        }
        return path;
    }
}
