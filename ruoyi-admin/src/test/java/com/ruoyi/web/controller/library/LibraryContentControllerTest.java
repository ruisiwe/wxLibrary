package com.ruoyi.web.controller.library;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.dto.BannerDto;
import com.ruoyi.library.dto.CategoryIconOptionDto;
import com.ruoyi.library.dto.DocumentOptionDto;
import com.ruoyi.library.dto.HomeData;
import com.ruoyi.library.dto.PageResult;
import com.ruoyi.library.service.DocumentService;
import com.ruoyi.library.service.BannerManagementService;
import com.ruoyi.library.service.HomeQueryService;
import com.ruoyi.web.controller.library.wx.WxApiExceptionHandler;
import com.ruoyi.web.controller.library.wx.WxPublicContentController;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LibraryContentControllerTest
{
    private HomeQueryService homeQueryService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        homeQueryService = mock(HomeQueryService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new WxPublicContentController(homeQueryService))
                .setControllerAdvice(new WxApiExceptionHandler())
                .build();
    }

    @Test
    void anonymousHomeUsesWxResponseEnvelope() throws Exception
    {
        BannerDto banner = new BannerDto();
        banner.setId(1L);
        banner.setDocumentId(9L);
        when(homeQueryService.getHome(1, 10)).thenReturn(new HomeData(
                Collections.singletonList(banner), Collections.emptyList(), Collections.emptyList()));

        mockMvc.perform(get("/wx/public/home").param("pageNum", "1").param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.message").value("操作成功"))
                .andExpect(jsonPath("$.data.banners[0].documentId").value(9L));
    }

    @Test
    void anonymousDocumentSearchReturnsPageContract() throws Exception
    {
        when(homeQueryService.searchDocuments("质量", 3L, 2, 20))
                .thenReturn(new PageResult<>(Collections.emptyList(), 0L, 2, 20));

        mockMvc.perform(get("/wx/public/documents")
                        .param("keyword", "质量").param("categoryId", "3")
                        .param("pageNum", "2").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(0))
                .andExpect(jsonPath("$.data.pageNum").value(2))
                .andExpect(jsonPath("$.data.items").isArray());
    }

    @Test
    void missingDocumentUsesSafeUnifiedChineseError() throws Exception
    {
        when(homeQueryService.getDocument(99L)).thenThrow(new ServiceException("文档不存在或已下架"));

        mockMvc.perform(get("/wx/public/documents/99"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40001))
                .andExpect(jsonPath("$.message").value("文档不存在或已下架"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }

    @Test
    void bannerDocumentOptionsReturnLightweightAvailabilityFields() throws Exception
    {
        DocumentService documentService = mock(DocumentService.class);
        DocumentOptionDto option = new DocumentOptionDto();
        option.setId(9L);
        option.setTitle("质量管理手册");
        option.setCategoryName("质量管理");
        option.setFileFormat("PDF");
        option.setDocumentSelectable(false);
        option.setAvailabilityStatus("DRAFT");
        when(documentService.listBannerDocumentOptions("质量", 1, 20))
                .thenReturn(new PageResult<>(Collections.singletonList(option), 1L, 1, 20));
        MockMvc bannerMockMvc = MockMvcBuilders
                .standaloneSetup(new LibraryBannerController(
                        documentService, mock(BannerManagementService.class))).build();

        bannerMockMvc.perform(get("/library/banner/document-options")
                        .param("keyword", "质量").param("pageNum", "1").param("pageSize", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].id").value(9L))
                .andExpect(jsonPath("$.data.items[0].title").value("质量管理手册"))
                .andExpect(jsonPath("$.data.items[0].categoryName").value("质量管理"))
                .andExpect(jsonPath("$.data.items[0].fileFormat").value("PDF"))
                .andExpect(jsonPath("$.data.items[0].documentSelectable").value(false))
                .andExpect(jsonPath("$.data.items[0].availabilityStatus").value("DRAFT"))
                .andExpect(jsonPath("$.data.items[0].originalObjectKey").doesNotExist());
    }

    @Test
    void categoryIconOptionsReturnNameLabelAndKeywords() throws Exception
    {
        DocumentService documentService = mock(DocumentService.class);
        when(documentService.listCategoryIconOptions()).thenReturn(Arrays.asList(
                new CategoryIconOptionDto("time", "时间", "时间 时钟 日期")));
        MockMvc categoryMockMvc = MockMvcBuilders
                .standaloneSetup(new LibraryCategoryController(documentService)).build();

        categoryMockMvc.perform(get("/library/category/icon-options"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].name").value("time"))
                .andExpect(jsonPath("$.data[0].label").value("时间"))
                .andExpect(jsonPath("$.data[0].keywords").value("时间 时钟 日期"));
    }

    @Test
    void managementControllersExposeApprovedPermissions() throws Exception
    {
        assertPermission(LibraryBannerController.class, "list", "library:banner:list");
        assertPermission(LibraryBannerController.class, "add", "library:banner:add");
        assertPermission(LibraryBannerController.class, "edit", "library:banner:edit");
        assertPermission(LibraryBannerController.class, "remove", "library:banner:remove");
        assertAuthorization(LibraryBannerController.class, "documentOptions",
                "@ss.hasAnyPermi('library:banner:add,library:banner:edit')");
        assertAuthorization(LibraryBannerController.class, "image",
                "@ss.hasAnyPermi('library:banner:list,library:banner:edit')");
        assertPermission(LibraryCategoryController.class, "list", "library:category:list");
        assertPermission(LibraryCategoryController.class, "iconOptions", "library:category:list");
        assertPermission(LibraryCategoryController.class, "add", "library:category:add");
        assertPermission(LibraryCategoryController.class, "edit", "library:category:edit");
        assertPermission(LibraryCategoryController.class, "remove", "library:category:remove");
        assertPermission(LibraryDocumentController.class, "list", "library:document:list");
        assertPermission(LibraryDocumentController.class, "add", "library:document:add");
        assertPermission(LibraryDocumentController.class, "edit", "library:document:edit");
        assertPermission(LibraryDocumentController.class, "remove", "library:document:remove");
        assertPermission(LibraryDocumentController.class, "publish", "library:document:publish");
        assertPermission(LibraryDocumentController.class, "unpublish", "library:document:publish");
        assertPermission(LibraryPointRuleController.class, "list", "library:points:rule");
        assertPermission(LibraryPointRuleController.class, "detail", "library:points:rule");
        assertPermission(LibraryPointRuleController.class, "edit", "library:points:rule");
        assertPermission(LibraryPointRecordController.class, "list", "library:points:record");
        assertPermission(LibraryVipPlanController.class, "list", "library:vip:plan");
        assertPermission(LibraryVipPlanController.class, "add", "library:vip:plan");
        assertPermission(LibraryVipPlanController.class, "edit", "library:vip:plan");
        assertPermission(LibraryVipBenefitController.class, "list", "library:vip:benefit:list");
        assertPermission(LibraryVipBenefitController.class, "add", "library:vip:benefit:add");
        assertPermission(LibraryVipBenefitController.class, "edit", "library:vip:benefit:edit");
        assertPermission(LibraryVipBenefitController.class, "remove", "library:vip:benefit:remove");
        assertPermission(LibraryVipPageConfigController.class, "detail",
                "library:vip:page-config:query");
        assertPermission(LibraryVipPageConfigController.class, "edit",
                "library:vip:page-config:edit");
        assertPermission(LibraryVipOperationController.class, "list", "library:vip:operation");
        assertPermission(LibraryVipOperationController.class, "userOptions", "library:vip:operation");
        assertPermission(LibraryVipOperationController.class, "open", "library:vip:operation");
        assertPermission(LibraryVipOperationController.class, "compensate", "library:vip:operation");
        assertPermission(LibraryVipOrderController.class, "list", "library:vip:order");
        assertPermission(LibraryVipRefundController.class, "list", "library:vip:refund");
        assertPermission(LibraryVipRefundController.class, "refund", "library:vip:refund");
        assertPermission(LibraryCourseController.class, "list", "library:course:list");
        assertPermission(LibraryCourseController.class, "add", "library:course:add");
        assertPermission(LibraryCourseController.class, "edit", "library:course:edit");
        assertPermission(LibraryCourseCodeController.class, "list", "library:course:code");
        assertPermission(LibraryCourseCodeController.class, "generate", "library:course:code");
    }

    private void assertPermission(Class<?> controllerClass, String methodName, String permission)
    {
        Method matched = null;
        for (Method method : controllerClass.getDeclaredMethods())
        {
            if (method.getName().equals(methodName))
            {
                matched = method;
                break;
            }
        }
        PreAuthorize annotation = matched == null ? null : matched.getAnnotation(PreAuthorize.class);
        assertEquals("@ss.hasPermi('" + permission + "')", annotation == null ? null : annotation.value());
    }

    private void assertAuthorization(Class<?> controllerClass, String methodName, String expression)
    {
        Method matched = null;
        for (Method method : controllerClass.getDeclaredMethods())
        {
            if (method.getName().equals(methodName))
            {
                matched = method;
                break;
            }
        }
        PreAuthorize annotation = matched == null ? null : matched.getAnnotation(PreAuthorize.class);
        assertEquals(expression, annotation == null ? null : annotation.value());
    }
}
