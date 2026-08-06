package com.ruoyi.web.controller.library;

import com.ruoyi.library.domain.WlCategory;
import com.ruoyi.library.service.DocumentConversionService;
import com.ruoyi.library.service.DocumentDeletionService;
import com.ruoyi.library.service.DocumentService;
import java.util.Arrays;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LibraryDocumentControllerTest
{
    private DocumentService documentService;
    private DocumentDeletionService deletionService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        documentService = mock(DocumentService.class);
        deletionService = mock(DocumentDeletionService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(new TestController(
                documentService, mock(DocumentConversionService.class), deletionService)).build();
    }

    @Test
    void categoryOptionsReturnDocumentCategoryNames() throws Exception
    {
        WlCategory category = new WlCategory();
        category.setId(2L);
        category.setName("质量管理");
        category.setStatus("0");
        when(documentService.listDocumentCategoryOptions(9L))
                .thenReturn(Collections.singletonList(category));

        mockMvc.perform(get("/library/document/category-options")
                        .param("currentCategoryId", "9"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].id").value(2))
                .andExpect(jsonPath("$.data[0].name").value("质量管理"))
                .andExpect(jsonPath("$.data[0].status").value("0"));

        verify(documentService).listDocumentCategoryOptions(9L);
    }

    @Test
    void categoryOptionsUseDocumentWritePermissions() throws Exception
    {
        PreAuthorize permission = LibraryDocumentController.class
                .getMethod("categoryOptions", Long.class)
                .getAnnotation(PreAuthorize.class);

        assertEquals("@ss.hasAnyPermi('library:document:add,library:document:edit')",
                permission.value());
    }

    @Test
    void removeUsesDocumentDeletionService() throws Exception
    {
        when(deletionService.remove(any(Long[].class), eq("admin"))).thenReturn(2);

        mockMvc.perform(delete("/library/document/7,8"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(deletionService).remove(
                argThat(ids -> Arrays.equals(ids, new Long[] {7L, 8L})), eq("admin"));
        verify(documentService, never()).removeDocuments(any(Long[].class), anyString());
    }

    @Test
    void unpublishDoesNotUseDocumentDeletionService() throws Exception
    {
        mockMvc.perform(put("/library/document/7/unpublish"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));

        verify(documentService).unpublishDocument(7L, "admin");
        verify(deletionService, never()).remove(any(Long[].class), anyString());
    }

    private static final class TestController extends LibraryDocumentController
    {
        private TestController(DocumentService documentService,
                DocumentConversionService conversionService, DocumentDeletionService deletionService)
        {
            super(documentService, conversionService, deletionService);
        }

        @Override
        public String getUsername()
        {
            return "admin";
        }
    }
}
