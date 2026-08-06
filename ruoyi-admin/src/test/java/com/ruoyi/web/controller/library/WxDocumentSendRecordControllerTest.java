package com.ruoyi.web.controller.library;

import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.service.DocumentSendRecordService;
import com.ruoyi.web.controller.library.wx.WxDocumentSendRecordController;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WxDocumentSendRecordControllerTest
{
    @AfterEach
    void clearContext()
    {
        WxUserContext.clear();
    }

    @Test
    void recordsSuccessfulSendForCurrentWechatUser() throws Exception
    {
        DocumentSendRecordService service = mock(DocumentSendRecordService.class);
        MockMvc mockMvc = MockMvcBuilders
                .standaloneSetup(new WxDocumentSendRecordController(service)).build();
        WxUserContext.set(9L);

        mockMvc.perform(post("/wx/documents/8/send-record")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"requestId\":\"send-8-1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.recorded").value(true));

        verify(service).record(9L, 8L, "send-8-1");
    }
}
