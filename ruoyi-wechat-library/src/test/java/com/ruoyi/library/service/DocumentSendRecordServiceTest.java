package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlDocumentSendRecord;
import com.ruoyi.library.mapper.WlDocumentSendRecordMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DocumentSendRecordServiceTest
{
    private DocumentAccessService accessService;
    private WlDocumentSendRecordMapper mapper;
    private DocumentSendRecordService service;

    @BeforeEach
    void setUp()
    {
        accessService = mock(DocumentAccessService.class);
        mapper = mock(WlDocumentSendRecordMapper.class);
        service = new DocumentSendRecordService(accessService, mapper);
    }

    @Test
    void recordsEachSuccessfulUnlockedSend()
    {
        when(mapper.insertRecord(any(WlDocumentSendRecord.class))).thenReturn(1);

        assertTrue(service.record(9L, 8L, "send-8-1"));

        ArgumentCaptor<WlDocumentSendRecord> captor =
                ArgumentCaptor.forClass(WlDocumentSendRecord.class);
        verify(mapper).insertRecord(captor.capture());
        assertEquals(9L, captor.getValue().getUserId());
        assertEquals(8L, captor.getValue().getDocumentId());
        assertEquals("send-8-1", captor.getValue().getRequestId());
        verify(accessService).validateOriginalFileSendPermission(9L, 8L);
    }

    @Test
    void repeatedMatchingRequestIsIdempotent()
    {
        WlDocumentSendRecord existing = record(9L, 8L, "send-8-1");
        when(mapper.selectByRequestId("send-8-1")).thenReturn(existing);

        assertTrue(service.record(9L, 8L, "send-8-1"));

        verify(accessService, never()).validateOriginalFileSendPermission(9L, 8L);
        verify(mapper, never()).insertRecord(any(WlDocumentSendRecord.class));
    }

    @Test
    void insertConflictUsesCurrentReadForIdempotentResult()
    {
        WlDocumentSendRecord existing = record(9L, 8L, "send-8-1");
        when(mapper.selectByRequestId("send-8-1")).thenReturn(null);
        when(mapper.insertRecord(any(WlDocumentSendRecord.class))).thenReturn(0);
        when(mapper.selectByRequestIdForUpdate("send-8-1")).thenReturn(existing);

        assertTrue(service.record(9L, 8L, "send-8-1"));

        verify(mapper).selectByRequestIdForUpdate("send-8-1");
    }

    @Test
    void rejectsRequestIdOwnedByAnotherSend()
    {
        when(mapper.selectByRequestId("send-8-1"))
                .thenReturn(record(10L, 8L, "send-8-1"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.record(9L, 8L, "send-8-1"));

        assertEquals("发送请求号已被使用", exception.getMessage());
        verify(mapper, never()).insertRecord(any(WlDocumentSendRecord.class));
    }

    @Test
    void requiresUnlockedDocumentAndValidRequestId()
    {
        assertEquals("发送请求号不能为空", assertThrows(ServiceException.class,
                () -> service.record(9L, 8L, " ")).getMessage());
        assertEquals("发送请求号不能超过64个字符", assertThrows(ServiceException.class,
                () -> service.record(9L, 8L, repeat('a', 65))).getMessage());

        doThrow(new ServiceException("请先兑换文档"))
                .when(accessService).validateOriginalFileSendPermission(9L, 8L);
        assertEquals("请先兑换文档", assertThrows(ServiceException.class,
                () -> service.record(9L, 8L, "send-8-2")).getMessage());
    }

    private WlDocumentSendRecord record(Long userId, Long documentId, String requestId)
    {
        WlDocumentSendRecord record = new WlDocumentSendRecord();
        record.setUserId(userId);
        record.setDocumentId(documentId);
        record.setRequestId(requestId);
        return record;
    }

    private String repeat(char value, int count)
    {
        StringBuilder result = new StringBuilder(count);
        for (int index = 0; index < count; index++) result.append(value);
        return result.toString();
    }
}
