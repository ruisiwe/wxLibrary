package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.dto.CategoryDto;
import com.ruoyi.library.dto.DocumentSummaryDto;
import com.ruoyi.library.dto.HomeData;
import com.ruoyi.library.dto.PageResult;
import com.ruoyi.library.mapper.WlBannerMapper;
import com.ruoyi.library.mapper.WlCategoryMapper;
import com.ruoyi.library.mapper.WlDocumentMapper;
import com.ruoyi.library.storage.PrivateFileUrlSigner;
import java.net.URL;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.ObjectProvider;

/** 匿名首页、分类和公开文档查询服务。 */
@Service
public class HomeQueryService
{
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;
    private static final Duration COVER_URL_TTL = Duration.ofMinutes(30);

    private final WlBannerMapper bannerMapper;
    private final WlCategoryMapper categoryMapper;
    private final WlDocumentMapper documentMapper;
    private final ObjectProvider<PrivateFileUrlSigner> signerProvider;

    public HomeQueryService(WlBannerMapper bannerMapper, WlCategoryMapper categoryMapper,
            WlDocumentMapper documentMapper, ObjectProvider<PrivateFileUrlSigner> signerProvider)
    {
        this.bannerMapper = bannerMapper;
        this.categoryMapper = categoryMapper;
        this.documentMapper = documentMapper;
        this.signerProvider = signerProvider;
    }

    public HomeData getHome(int pageNum, int pageSize)
    {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        long offset = ((long) safePageNum - 1L) * safePageSize;
        List<DocumentSummaryDto> documents = documentMapper.selectPublishedDocuments(
                null, null, offset, safePageSize);
        signCovers(documents);
        return new HomeData(bannerMapper.selectPublicBanners(new Date()),
                categoryMapper.selectPublicCategories(), documents);
    }

    public List<CategoryDto> listCategories()
    {
        return categoryMapper.selectPublicCategories();
    }

    public PageResult<DocumentSummaryDto> searchDocuments(String keyword, Long categoryId,
            int pageNum, int pageSize)
    {
        if (categoryId != null && categoryId <= 0) throw new ServiceException("文档分类编号不正确");
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        long offset = ((long) safePageNum - 1L) * safePageSize;
        String escapedKeyword = escapeLike(keyword);
        long total = documentMapper.countPublishedDocuments(escapedKeyword, categoryId);
        List<DocumentSummaryDto> items = total == 0L
                ? java.util.Collections.<DocumentSummaryDto>emptyList()
                : documentMapper.selectPublishedDocuments(escapedKeyword, categoryId, offset, safePageSize);
        signCovers(items);
        return new PageResult<>(items, total, safePageNum, safePageSize);
    }

    public DocumentSummaryDto getDocument(Long id)
    {
        if (id == null) throw new ServiceException("文档编号不能为空");
        DocumentSummaryDto document = documentMapper.selectPublishedDocumentById(id);
        if (document == null) throw new ServiceException("文档不存在或已下架");
        signCover(document);
        return document;
    }

    private void signCovers(List<DocumentSummaryDto> documents)
    {
        if (documents == null) return;
        for (DocumentSummaryDto document : documents) signCover(document);
    }

    private void signCover(DocumentSummaryDto document)
    {
        if (document == null || document.getCoverUrl() == null || document.getCoverUrl().trim().isEmpty()) return;
        String cover = document.getCoverUrl().trim();
        if (cover.startsWith("https://") || cover.startsWith("http://")) return;
        PrivateFileUrlSigner signer = signerProvider.getIfAvailable();
        if (signer == null) throw new ServiceException("缩略图服务暂不可用，请稍后重试");
        URL url = signer.signGetUrl(cover, COVER_URL_TTL, null);
        if (url == null) throw new ServiceException("缩略图服务暂不可用，请稍后重试");
        document.setCoverUrl(url.toString());
    }

    private int normalizePageNum(int pageNum)
    {
        return pageNum < 1 ? 1 : pageNum;
    }

    private int normalizePageSize(int pageSize)
    {
        if (pageSize < 1) return DEFAULT_PAGE_SIZE;
        return Math.min(pageSize, MAX_PAGE_SIZE);
    }

    private String escapeLike(String keyword)
    {
        if (keyword == null || keyword.trim().isEmpty()) return null;
        return keyword.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
