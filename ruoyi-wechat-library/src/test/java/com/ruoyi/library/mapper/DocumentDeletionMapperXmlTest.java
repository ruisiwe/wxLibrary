package com.ruoyi.library.mapper;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class DocumentDeletionMapperXmlTest
{
    @Test
    void deletionSnapshotLocksCurrentDocumentRows() throws Exception
    {
        String xml = new String(Files.readAllBytes(Paths.get(
                "src/main/resources/mapper/library/WlDocumentMapper.xml")), StandardCharsets.UTF_8);

        assertTrue(xml.contains("<select id=\"selectDocumentsForUpdate\""));
        assertTrue(xml.contains("where id in"));
        assertTrue(xml.contains("and del_flag = '0'"));
        assertTrue(xml.contains("for update"));
    }
}
