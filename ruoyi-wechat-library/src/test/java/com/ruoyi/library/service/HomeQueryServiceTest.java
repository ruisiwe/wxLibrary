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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class HomeQueryServiceTest
{
    private WlBannerMapper bannerMapper;
    private WlCategoryMapper categoryMapper;
    private WlDocumentMapper documentMapper;
    private HomeQueryService service;

    @BeforeEach
    void setUp()
    {
        bannerMapper = mock(WlBannerMapper.class);
        categoryMapper = mock(WlCategoryMapper.class);
        documentMapper = mock(WlDocumentMapper.class);
        service = new HomeQueryService(bannerMapper, categoryMapper, documentMapper);
    }

    @Test
    void anonymousHomeReturnsOnlyMapperFilteredPublicMetadata()
    {
        BannerDto banner = new BannerDto();
        banner.setId(1L);
        banner.setDocumentId(11L);
        CategoryDto category = new CategoryDto();
        category.setId(2L);
        DocumentSummaryDto document = document(11L, "质量管理手册");
        when(bannerMapper.selectPublicBanners(any(Date.class))).thenReturn(Collections.singletonList(banner));
        when(categoryMapper.selectPublicCategories()).thenReturn(Collections.singletonList(category));
        when(documentMapper.selectPublishedDocuments(null, null, 0, 10))
                .thenReturn(Collections.singletonList(document));

        HomeData result = service.getHome(1, 10);

        assertEquals(Collections.singletonList(banner), result.getBanners());
        assertEquals(Collections.singletonList(category), result.getCategories());
        assertEquals(Collections.singletonList(document), result.getDocuments());
        verify(bannerMapper).selectPublicBanners(any(Date.class));
    }

    @Test
    void searchEscapesLikeWildcardsAndUsesBoundedPagination()
    {
        when(documentMapper.countPublishedDocuments("50\\%\\_\\\\", 3L)).thenReturn(1L);
        when(documentMapper.selectPublishedDocuments("50\\%\\_\\\\", 3L, 50, 50))
                .thenReturn(Collections.singletonList(document(9L, "匹配文档")));

        PageResult<DocumentSummaryDto> result = service.searchDocuments(" 50%_\\ ", 3L, 2, 1000);

        assertEquals(2, result.getPageNum());
        assertEquals(50, result.getPageSize());
        assertEquals(1L, result.getTotal());
        assertEquals(1, result.getItems().size());
    }

    @Test
    void publicDocumentDtoCannotExposePrivateObjectKeys()
    {
        assertFalse(Arrays.stream(DocumentSummaryDto.class.getMethods())
                .map(Method::getName)
                .anyMatch(name -> name.toLowerCase().contains("objectkey")));
    }

    @Test
    void missingOrUnpublishedDocumentUsesSafeChineseMessage()
    {
        when(documentMapper.selectPublishedDocumentById(99L)).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.getDocument(99L));

        assertEquals("文档不存在或已下架", exception.getMessage());
    }

    private DocumentSummaryDto document(Long id, String title)
    {
        DocumentSummaryDto document = new DocumentSummaryDto();
        document.setId(id);
        document.setTitle(title);
        return document;
    }
}
