package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.dto.CategoryDto;
import com.ruoyi.library.dto.DocumentSummaryDto;
import com.ruoyi.library.dto.HomeData;
import com.ruoyi.library.dto.PageResult;
import com.ruoyi.library.mapper.WlBannerMapper;
import com.ruoyi.library.mapper.WlCategoryMapper;
import com.ruoyi.library.mapper.WlDocumentMapper;
import java.util.Date;
import java.util.List;
import org.springframework.stereotype.Service;

/** 匿名首页、分类和公开文档查询服务。 */
@Service
public class HomeQueryService
{
    private static final int DEFAULT_PAGE_SIZE = 10;
    private static final int MAX_PAGE_SIZE = 50;

    private final WlBannerMapper bannerMapper;
    private final WlCategoryMapper categoryMapper;
    private final WlDocumentMapper documentMapper;

    public HomeQueryService(WlBannerMapper bannerMapper, WlCategoryMapper categoryMapper,
            WlDocumentMapper documentMapper)
    {
        this.bannerMapper = bannerMapper;
        this.categoryMapper = categoryMapper;
        this.documentMapper = documentMapper;
    }

    public HomeData getHome(int pageNum, int pageSize)
    {
        int safePageNum = normalizePageNum(pageNum);
        int safePageSize = normalizePageSize(pageSize);
        long offset = ((long) safePageNum - 1L) * safePageSize;
        return new HomeData(bannerMapper.selectPublicBanners(new Date()),
                categoryMapper.selectPublicCategories(),
                documentMapper.selectPublishedDocuments(null, null, offset, safePageSize));
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
        return new PageResult<>(items, total, safePageNum, safePageSize);
    }

    public DocumentSummaryDto getDocument(Long id)
    {
        if (id == null) throw new ServiceException("文档编号不能为空");
        DocumentSummaryDto document = documentMapper.selectPublishedDocumentById(id);
        if (document == null) throw new ServiceException("文档不存在或已下架");
        return document;
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
