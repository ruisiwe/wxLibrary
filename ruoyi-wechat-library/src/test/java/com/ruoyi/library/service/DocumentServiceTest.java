package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlBanner;
import com.ruoyi.library.domain.WlCategory;
import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.mapper.WlBannerMapper;
import com.ruoyi.library.mapper.WlCategoryMapper;
import com.ruoyi.library.mapper.WlDocumentMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentServiceTest
{
    private WlBannerMapper bannerMapper;
    private WlCategoryMapper categoryMapper;
    private WlDocumentMapper documentMapper;
    private DocumentService service;

    @BeforeEach
    void setUp()
    {
        bannerMapper = mock(WlBannerMapper.class);
        categoryMapper = mock(WlCategoryMapper.class);
        documentMapper = mock(WlDocumentMapper.class);
        service = new DocumentService(bannerMapper, categoryMapper, documentMapper);
        WlCategory enabled = new WlCategory();
        enabled.setId(3L);
        enabled.setStatus("0");
        when(categoryMapper.selectCategoryById(3L)).thenReturn(enabled);
    }

    @Test
    void bannerWriteRejectsDocumentThatIsNotPublished()
    {
        WlDocument draft = validDocument();
        draft.setPublishStatus("DRAFT");
        when(documentMapper.selectDocumentById(8L)).thenReturn(draft);
        WlBanner banner = new WlBanner();
        banner.setTitle("首页推荐");
        banner.setImageUrl("https://static.example/banner.png");
        banner.setDocumentId(8L);

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.addBanner(banner, "admin"));

        assertEquals("宣传图片只能关联已上架文档", exception.getMessage());
        verify(bannerMapper, never()).insertBanner(banner);
    }

    @Test
    void invalidFormatAndPreviewBoundaryAreRejected()
    {
        WlDocument document = validDocument();
        document.setFileFormat("XLSX");
        assertEquals("文档格式不支持", assertThrows(ServiceException.class,
                () -> service.addDocument(document, "admin")).getMessage());

        document.setFileFormat("PDF");
        document.setPageCount(4);
        document.setPreviewPages(5);
        assertEquals("试读页数不能大于文档总页数", assertThrows(ServiceException.class,
                () -> service.addDocument(document, "admin")).getMessage());
    }

    @Test
    void publishRequiresSuccessfulConversionAndEnabledCategory()
    {
        WlDocument pending = validDocument();
        pending.setId(7L);
        pending.setConversionStatus("PENDING");
        when(documentMapper.selectDocumentById(7L)).thenReturn(pending);

        assertEquals("文档转换成功后才能上架", assertThrows(ServiceException.class,
                () -> service.publishDocument(7L, "admin")).getMessage());
        verify(documentMapper, never()).updatePublishStatus(7L, "PUBLISHED", "admin");

        pending.setConversionStatus("SUCCESS");
        WlCategory disabled = new WlCategory();
        disabled.setId(3L);
        disabled.setStatus("1");
        when(categoryMapper.selectCategoryById(3L)).thenReturn(disabled);
        assertEquals("启用文档分类后才能上架", assertThrows(ServiceException.class,
                () -> service.publishDocument(7L, "admin")).getMessage());
    }

    @Test
    void publishedDocumentCanBeUnpublishedIdempotently()
    {
        WlDocument published = validDocument();
        published.setId(7L);
        published.setPublishStatus("PUBLISHED");
        when(documentMapper.selectDocumentById(7L)).thenReturn(published);
        when(documentMapper.updatePublishStatus(7L, "DRAFT", "admin")).thenReturn(1);

        assertEquals(1, service.unpublishDocument(7L, "admin"));
        verify(documentMapper).updatePublishStatus(7L, "DRAFT", "admin");
    }

    @Test
    void fixedAllCategoryCannotBeStoredAsOrdinaryCategory()
    {
        WlCategory category = new WlCategory();
        category.setName("全部分类");

        assertEquals("全部分类是固定入口，不能作为普通分类保存",
                assertThrows(ServiceException.class,
                        () -> service.addCategory(category, "admin")).getMessage());
    }

    @Test
    void newDocumentNormalizesNonNullColumnsAndForcesDraftPendingState()
    {
        WlDocument document = validDocument();
        document.setTitle(" 质量管理手册 ");
        document.setSummary(" ");
        document.setTags(null);
        document.setUploaderName(" 资料组 ");
        document.setFileFormat(" pdf ");
        document.setSortOrder(null);
        when(documentMapper.insertDocument(any(WlDocument.class))).thenReturn(1);

        assertEquals(1, service.addDocument(document, "admin"));

        ArgumentCaptor<WlDocument> captor = ArgumentCaptor.forClass(WlDocument.class);
        verify(documentMapper).insertDocument(captor.capture());
        WlDocument saved = captor.getValue();
        assertEquals("质量管理手册", saved.getTitle());
        assertEquals("", saved.getSummary());
        assertEquals("", saved.getTags());
        assertEquals("资料组", saved.getUploaderName());
        assertEquals("PDF", saved.getFileFormat());
        assertEquals("DRAFT", saved.getPublishStatus());
        assertEquals("PENDING", saved.getConversionStatus());
        assertEquals(0, saved.getSortOrder());
    }

    @Test
    void concurrentPublishPreventsDocumentDeletion()
    {
        WlDocument draft = validDocument();
        draft.setId(7L);
        when(documentMapper.selectDocumentById(7L)).thenReturn(draft);
        when(documentMapper.deleteDocuments(new Long[] {7L}, "admin")).thenReturn(0);

        assertEquals("文档状态已变化，请刷新后重试", assertThrows(ServiceException.class,
                () -> service.removeDocuments(new Long[] {7L}, "admin")).getMessage());
    }

    private WlDocument validDocument()
    {
        WlDocument document = new WlDocument();
        document.setCategoryId(3L);
        document.setTitle("质量管理手册");
        document.setSummary("文档摘要");
        document.setUploaderName("实验室资料组");
        document.setFileFormat("PDF");
        document.setFileSize(1024L);
        document.setPageCount(10);
        document.setPointPrice(2L);
        document.setPreviewPages(3);
        document.setOriginalObjectKey("documents/original.pdf");
        document.setConversionStatus("SUCCESS");
        document.setPublishStatus("DRAFT");
        return document;
    }
}
