package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.dto.DocumentSummaryDto;
import com.ruoyi.library.storage.PrivateFileUrlSigner;
import java.net.URL;
import java.time.Duration;
import java.util.List;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** 为小程序文档缩略图生成短时完整访问地址。 */
@Service
public class DocumentCoverUrlService
{
    private static final Duration COVER_URL_TTL = Duration.ofMinutes(30);

    private final ObjectProvider<PrivateFileUrlSigner> signerProvider;

    public DocumentCoverUrlService(ObjectProvider<PrivateFileUrlSigner> signerProvider)
    {
        this.signerProvider = signerProvider;
    }

    public void signCovers(List<DocumentSummaryDto> documents)
    {
        if (documents == null) return;
        for (DocumentSummaryDto document : documents) signCover(document);
    }

    public void signCover(DocumentSummaryDto document)
    {
        if (document == null || document.getCoverUrl() == null
                || document.getCoverUrl().trim().isEmpty()) return;
        String coverUrl = document.getCoverUrl().trim();
        if (coverUrl.startsWith("https://") || coverUrl.startsWith("http://")) return;
        try
        {
            PrivateFileUrlSigner signer = signerProvider.getIfAvailable();
            if (signer == null) throw new ServiceException("缩略图服务暂不可用，请稍后重试");
            URL url = signer.signGetUrl(coverUrl, COVER_URL_TTL, null);
            if (url == null) throw new ServiceException("缩略图服务暂不可用，请稍后重试");
            document.setCoverUrl(url.toString());
        }
        catch (RuntimeException exception)
        {
            throw new ServiceException("缩略图服务暂不可用，请稍后重试");
        }
    }
}
