package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.dto.BannerDto;
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
    private static final Duration BANNER_URL_TTL = Duration.ofMinutes(30);

    private final WlBannerMapper bannerMapper;
    private final WlCategoryMapper categoryMapper;
    private final WlDocumentMapper documentMapper;
    private final ObjectProvider<PrivateFileUrlSigner> signerProvider;
    private final DocumentCoverUrlService coverUrlService;

    public HomeQueryService(WlBannerMapper bannerMapper, WlCategoryMapper categoryMapper,
            WlDocumentMapper documentMapper, ObjectProvider<PrivateFileUrlSigner> signerProvider,
            DocumentCoverUrlService coverUrlService)
    {
        this.bannerMapper = bannerMapper;
        this.categoryMapper = categoryMapper;
        this.documentMapper = documentMapper;
        this.signerProvider = signerProvider;
        this.coverUrlService = coverUrlService;
    }

    public HomeData getHome(int pageNum, int pageSize)
    {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        long offset = ((long) safePageNum - 1L) * safePageSize;
        List<DocumentSummaryDto> documents = documentMapper.selectPublishedDocuments(
                null, null, offset, safePageSize);
        coverUrlService.signCovers(documents);
        List<BannerDto> banners = bannerMapper.selectPublicBanners(new Date());
        signBanners(banners);
        return new HomeData(banners,
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
        coverUrlService.signCovers(items);
        return new PageResult<>(items, total, safePageNum, safePageSize);
    }

    public DocumentSummaryDto getDocument(Long id)
    {
        if (id == null) throw new ServiceException("文档编号不能为空");
        DocumentSummaryDto document = documentMapper.selectPublishedDocumentById(id);
        if (document == null) throw new ServiceException("文档不存在或已下架");
        coverUrlService.signCover(document);
        return document;
    }

    private void signBanners(List<BannerDto> banners)
    {
        if (banners == null) return;
        for (BannerDto banner : banners) signBanner(banner);
    }

    private void signBanner(BannerDto banner)
    {
        if (banner == null || banner.getImageUrl() == null
                || banner.getImageUrl().trim().isEmpty()) return;
        String imageUrl = banner.getImageUrl().trim();
        if (imageUrl.startsWith("https://") || imageUrl.startsWith("http://")) return;
        try
        {
            PrivateFileUrlSigner signer = signerProvider.getIfAvailable();
            if (signer == null) throw new ServiceException("轮播图图片服务暂不可用，请稍后重试");
            URL url = signer.signGetUrl(imageUrl, BANNER_URL_TTL, null);
            if (url == null) throw new ServiceException("轮播图图片服务暂不可用，请稍后重试");
            banner.setImageUrl(url.toString());
        }
        catch (RuntimeException exception)
        {
            throw new ServiceException("轮播图图片服务暂不可用，请稍后重试");
        }
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
