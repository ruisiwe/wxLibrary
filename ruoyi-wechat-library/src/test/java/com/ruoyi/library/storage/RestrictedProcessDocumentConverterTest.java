package com.ruoyi.library.storage;

import com.ruoyi.library.config.DocumentConversionProperties;
import java.nio.file.Files;
import java.nio.file.Path;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RestrictedProcessDocumentConverterTest
{
    @TempDir
    Path tempDir;

    @Test
    void pdfConversionCreatesPreviewAndCleansWholeTaskDirectory() throws Exception
    {
        Path source = tempDir.resolve("source.pdf");
        try (PDDocument pdf = new PDDocument())
        {
            pdf.addPage(new PDPage());
            pdf.addPage(new PDPage());
            pdf.addPage(new PDPage());
            pdf.save(source.toFile());
        }
        DocumentConversionProperties properties = new DocumentConversionProperties();
        properties.setTempDirectory(tempDir.toString());
        properties.setMaxInputBytes(1024L * 1024L);
        properties.setMaxOutputBytes(1024L * 1024L);
        RestrictedProcessDocumentConverter converter = new RestrictedProcessDocumentConverter(properties);

        Path workDirectory;
        try (DocumentConverter.ConversionArtifacts result = converter.convert(
                source.toUri().toURL(), "PDF", 1, 1024L * 1024L))
        {
            assertEquals(3, result.getPageCount());
            try (PDDocument preview = Loader.loadPDF(result.getPreviewPdf().toFile()))
            {
                assertEquals(1, preview.getNumberOfPages());
            }
            workDirectory = result.getFullPdf().getParent();
            assertTrue(Files.exists(workDirectory));
        }

        assertFalse(Files.exists(workDirectory));
    }
}
