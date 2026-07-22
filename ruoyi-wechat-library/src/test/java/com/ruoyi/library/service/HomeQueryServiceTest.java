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
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.ObjectProvider;

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
    private PrivateFileUrlSigner signer;
    private ObjectProvider<PrivateFileUrlSigner> signerProvider;

    @BeforeEach
    void setUp()
    {
        bannerMapper = mock(WlBannerMapper.class);
        categoryMapper = mock(WlCategoryMapper.class);
        documentMapper = mock(WlDocumentMapper.class);
        signer = mock(PrivateFileUrlSigner.class);
        @SuppressWarnings("unchecked") ObjectProvider<PrivateFileUrlSigner> provider = mock(ObjectProvider.class);
        signerProvider = provider;
        when(signerProvider.getIfAvailable()).thenReturn(signer);
        service = new HomeQueryService(bannerMapper, categoryMapper, documentMapper, signerProvider);
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

    @Test
    void privateThumbnailObjectKeyIsReturnedAsShortLivedUrl() throws Exception
    {
        DocumentSummaryDto document = document(11L, "质量管理手册");
        document.setCoverUrl("documents/session/thumbnail/v1.jpg");
        when(documentMapper.selectPublishedDocumentById(11L)).thenReturn(document);
        when(signer.signGetUrl(eq("documents/session/thumbnail/v1.jpg"), any(Duration.class), eq(null)))
                .thenReturn(new URL("https://temporary.example/cover"));

        DocumentSummaryDto result = service.getDocument(11L);

        assertEquals("https://temporary.example/cover", result.getCoverUrl());
    }

    @Test
    void existingHttpsThumbnailDoesNotUseCosSigner()
    {
        DocumentSummaryDto document = document(11L, "质量管理手册");
        document.setCoverUrl("https://legacy.example/cover.jpg");
        when(documentMapper.selectPublishedDocumentById(11L)).thenReturn(document);

        assertEquals("https://legacy.example/cover.jpg", service.getDocument(11L).getCoverUrl());
        verify(signer, org.mockito.Mockito.never()).signGetUrl(any(), any(), any());
    }

    @Test
    void homeSignsPrivateBannerKeysWithoutChangingLegacyUrls() throws Exception
    {
        BannerDto privateBanner = banner("banners/a/image.jpg");
        BannerDto legacyBanner = banner("https://legacy.example/banner.jpg");
        when(bannerMapper.selectPublicBanners(any(Date.class)))
                .thenReturn(Arrays.asList(privateBanner, legacyBanner));
        when(documentMapper.selectPublishedDocuments(null, null, 0, 10))
                .thenReturn(Collections.emptyList());
        when(signer.signGetUrl("banners/a/image.jpg", Duration.ofMinutes(30), null))
                .thenReturn(new URL("https://temporary.example/banner.jpg"));

        HomeData result = service.getHome(1, 10);

        assertEquals("https://temporary.example/banner.jpg",
                result.getBanners().get(0).getImageUrl());
        assertEquals("https://legacy.example/banner.jpg",
                result.getBanners().get(1).getImageUrl());
        verify(signer).signGetUrl("banners/a/image.jpg", Duration.ofMinutes(30), null);
    }

    @Test
    void missingBannerSignerUsesSafeChineseMessage()
    {
        when(bannerMapper.selectPublicBanners(any(Date.class)))
                .thenReturn(Collections.singletonList(banner("banners/a/image.jpg")));
        when(documentMapper.selectPublishedDocuments(null, null, 0, 10))
                .thenReturn(Collections.emptyList());
        when(signerProvider.getIfAvailable()).thenReturn(null);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.getHome(1, 10));

        assertEquals("轮播图图片服务暂不可用，请稍后重试", exception.getMessage());
    }

    private BannerDto banner(String imageUrl)
    {
        BannerDto banner = new BannerDto();
        banner.setId(1L);
        banner.setDocumentId(11L);
        banner.setImageUrl(imageUrl);
        return banner;
    }

    private DocumentSummaryDto document(Long id, String title)
    {
        DocumentSummaryDto document = new DocumentSummaryDto();
        document.setId(id);
        document.setTitle(title);
        return document;
    }
}
