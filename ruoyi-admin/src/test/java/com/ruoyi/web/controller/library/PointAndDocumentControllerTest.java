package com.ruoyi.web.controller.library;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.dto.DocumentUnlockResult;
import com.ruoyi.library.dto.FileAuthorization;
import com.ruoyi.library.dto.FileDisclaimerDto;
import com.ruoyi.library.dto.OriginalFileRequest;
import com.ruoyi.library.service.DocumentAccessService;
import com.ruoyi.library.service.PointService;
import com.ruoyi.web.controller.library.wx.WxApiExceptionHandler;
import com.ruoyi.web.controller.library.wx.WxDocumentController;
import com.ruoyi.web.controller.library.wx.WxPointController;
import java.lang.reflect.Method;
import java.util.Arrays;
import javax.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class PointAndDocumentControllerTest
{
    private DocumentAccessService documentAccessService;
    private PointService pointService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        documentAccessService = mock(DocumentAccessService.class);
        pointService = mock(PointService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new WxDocumentController(documentAccessService), new WxPointController(pointService))
                .setControllerAdvice(new WxApiExceptionHandler())
                .build();
        WxUserContext.set(11L);
    }

    @AfterEach
    void tearDown() { WxUserContext.clear(); }

    @Test
    void unlockUsesRequestIdAndWxResponseEnvelope() throws Exception
    {
        when(documentAccessService.unlock(11L, 22L, "request-1", false))
                .thenReturn(new DocumentUnlockResult(22L, true, 20L, 30L));

        mockMvc.perform(post("/wx/documents/22/unlock").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"request-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.unlocked").value(true))
                .andExpect(jsonPath("$.data.pointBalance").value(30));
        verify(documentAccessService).unlock(11L, 22L, "request-1", false);
    }

    @Test
    void unlockPropagatesFreeOnlyFlag() throws Exception
    {
        when(documentAccessService.unlock(11L, 22L, "request-free", true))
                .thenReturn(new DocumentUnlockResult(22L, true, 0L, 30L));

        mockMvc.perform(post("/wx/documents/22/unlock").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"request-free\",\"freeOnly\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.spentPoints").value(0));

        verify(documentAccessService).unlock(11L, 22L, "request-free", true);
    }

    @Test
    void originalFileEndpointReturnsOnlyFilenameAndTemporaryUrl() throws Exception
    {
        when(documentAccessService.authorizeOriginalFile(org.mockito.ArgumentMatchers.eq(11L),
                org.mockito.ArgumentMatchers.eq(22L), org.mockito.ArgumentMatchers.any(OriginalFileRequest.class),
                org.mockito.ArgumentMatchers.eq("127.0.0.1")))
                .thenReturn(new FileAuthorization("质量管理手册.docx",
                        "https://temporary.example/file", 300L));

        mockMvc.perform(post("/wx/documents/22/original").contentType(MediaType.APPLICATION_JSON)
                .content("{\"agreementId\":9,\"agreementVersion\":\"f1\",\"confirmed\":true}")
                .with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.fileName").value("质量管理手册.docx"))
                .andExpect(jsonPath("$.data.url").value("https://temporary.example/file"))
                .andExpect(jsonPath("$.data.expiresInSeconds").value(300));

        Method original = Arrays.stream(WxDocumentController.class.getDeclaredMethods())
                .filter(method -> method.getName().equals("original"))
                .findFirst().orElseThrow(AssertionError::new);
        assertEquals(Arrays.asList(Long.class, OriginalFileRequest.class, HttpServletRequest.class),
                Arrays.asList(original.getParameterTypes()));
    }

    @Test
    void fileDisclaimerReturnsBackendContentAndSuppressionState() throws Exception
    {
        FileDisclaimerDto disclaimer = new FileDisclaimerDto();
        disclaimer.setAgreementId(9L);
        disclaimer.setAgreementVersion("f1");
        disclaimer.setTitle("文件发送免责声明");
        disclaimer.setContent("后台配置正文");
        disclaimer.setReminderSuppressed(false);
        when(documentAccessService.fileDisclaimer(11L)).thenReturn(disclaimer);

        mockMvc.perform(get("/wx/documents/file-disclaimer"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content").value("后台配置正文"))
                .andExpect(jsonPath("$.data.reminderSuppressed").value(false));
    }

    @Test
    void adRewardAcceptsOnlyCompletionBusinessNumber() throws Exception
    {
        WlPointRecord record = new WlPointRecord();
        record.setChangePoints(1L);
        record.setAfterBalance(9L);
        when(pointService.rewardAd(11L, "ad-5")).thenReturn(record);

        mockMvc.perform(post("/wx/points/ad-reward").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"adBizNo\":\"ad-5\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.changePoints").value(1))
                .andExpect(jsonPath("$.data.afterBalance").value(9));
    }

    @Test
    void accessErrorUsesUnifiedSafeChineseResponse() throws Exception
    {
        when(documentAccessService.authorizePreview(11L, 22L, "127.0.0.1"))
                .thenThrow(new ServiceException("文档暂未生成试读文件"));

        mockMvc.perform(get("/wx/documents/22/preview").with(request -> {
                    request.setRemoteAddr("127.0.0.1");
                    return request;
                }))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("文档暂未生成试读文件"))
                .andExpect(jsonPath("$.data").value(nullValue()));
    }
}
