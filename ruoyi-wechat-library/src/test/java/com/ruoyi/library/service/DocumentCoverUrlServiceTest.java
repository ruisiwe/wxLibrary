package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.dto.DocumentSummaryDto;
import com.ruoyi.library.storage.PrivateFileUrlSigner;
import java.net.URL;
import java.time.Duration;
import java.util.Arrays;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentCoverUrlServiceTest
{
    private PrivateFileUrlSigner signer;
    private ObjectProvider<PrivateFileUrlSigner> signerProvider;
    private DocumentCoverUrlService service;

    @SuppressWarnings("unchecked")
    @BeforeEach
    void setUp()
    {
        signer = mock(PrivateFileUrlSigner.class);
        signerProvider = mock(ObjectProvider.class);
        when(signerProvider.getIfAvailable()).thenReturn(signer);
        service = new DocumentCoverUrlService(signerProvider);
    }

    @Test
    void signsRelativeCoverForThirtyMinutes() throws Exception
    {
        DocumentSummaryDto document = document("documents/session/thumbnail/v1.jpg");
        when(signer.signGetUrl(eq("documents/session/thumbnail/v1.jpg"),
                eq(Duration.ofMinutes(30)), eq(null)))
                .thenReturn(new URL("https://temporary.example/cover"));

        service.signCover(document);

        assertEquals("https://temporary.example/cover", document.getCoverUrl());
        verify(signer).signGetUrl("documents/session/thumbnail/v1.jpg",
                Duration.ofMinutes(30), null);
    }

    @Test
    void leavesEmptyAndAbsoluteCoversUnchanged()
    {
        DocumentSummaryDto empty = document("  ");
        DocumentSummaryDto http = document("http://legacy.example/cover.jpg");
        DocumentSummaryDto https = document("https://legacy.example/cover.jpg");

        service.signCovers(Arrays.asList(empty, http, https));

        assertEquals("  ", empty.getCoverUrl());
        assertEquals("http://legacy.example/cover.jpg", http.getCoverUrl());
        assertEquals("https://legacy.example/cover.jpg", https.getCoverUrl());
        verify(signer, never()).signGetUrl(any(), any(), any());
    }

    @Test
    void missingSignerUsesSafeChineseMessage()
    {
        when(signerProvider.getIfAvailable()).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.signCover(document("documents/session/thumbnail/v1.jpg")));

        assertEquals("缩略图服务暂不可用，请稍后重试", exception.getMessage());
    }

    private DocumentSummaryDto document(String coverUrl)
    {
        DocumentSummaryDto document = new DocumentSummaryDto();
        document.setCoverUrl(coverUrl);
        return document;
    }
}
