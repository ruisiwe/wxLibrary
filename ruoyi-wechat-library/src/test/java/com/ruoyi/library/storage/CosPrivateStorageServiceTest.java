package com.ruoyi.library.storage;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.GeneratePresignedUrlRequest;
import com.qcloud.cos.model.PutObjectRequest;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.config.CosProperties;
import java.io.ByteArrayInputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CosPrivateStorageServiceTest
{
    @Test
    void uploadsPrivateObjectAndSignsSafeDownloadName() throws Exception
    {
        CosProperties properties = new CosProperties();
        properties.setBucket("private-bucket-123");
        COSClient client = mock(COSClient.class);
        URL signed = new URL("https://temporary.example/file");
        when(client.generatePresignedUrl(any(GeneratePresignedUrlRequest.class))).thenReturn(signed);
        CosPrivateStorageService service = new CosPrivateStorageService(properties, client);
        byte[] content = "%PDF-test".getBytes(StandardCharsets.UTF_8);

        assertEquals("documents/7/full/v1.pdf", service.putPrivateObject(
                "documents/7/full/v1.pdf", new ByteArrayInputStream(content), content.length, "application/pdf"));
        assertEquals(signed, service.signGetUrl("documents/7/full/v1.pdf",
                Duration.ofMinutes(5), "质量手册.pdf"));

        ArgumentCaptor<PutObjectRequest> put = ArgumentCaptor.forClass(PutObjectRequest.class);
        verify(client).putObject(put.capture());
        assertEquals("private-bucket-123", put.getValue().getBucketName());
        assertEquals("application/pdf", put.getValue().getMetadata().getContentType());
        ArgumentCaptor<GeneratePresignedUrlRequest> sign = ArgumentCaptor.forClass(GeneratePresignedUrlRequest.class);
        verify(client).generatePresignedUrl(sign.capture());
        assertNotNull(sign.getValue().getResponseHeaders().getContentDisposition());
    }

    @Test
    void rejectsTraversalObjectKeyBeforeCallingCos()
    {
        CosProperties properties = new CosProperties();
        properties.setBucket("private-bucket-123");
        COSClient client = mock(COSClient.class);
        CosPrivateStorageService service = new CosPrivateStorageService(properties, client);

        assertEquals("COS 对象键不正确", assertThrows(ServiceException.class,
                () -> service.signGetUrl("documents/../secret", Duration.ofMinutes(5), null)).getMessage());
        verify(client, never()).generatePresignedUrl(any(GeneratePresignedUrlRequest.class));
    }
}
