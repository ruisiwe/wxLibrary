package com.ruoyi.library.service;

import java.util.Date;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlDocumentSendRecord;
import com.ruoyi.library.mapper.WlDocumentSendRecordMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 校验原文件发送权限并幂等记录发送成功行为。 */
@Service
public class DocumentSendRecordService
{
    private final DocumentAccessService accessService;
    private final WlDocumentSendRecordMapper recordMapper;

    public DocumentSendRecordService(DocumentAccessService accessService,
            WlDocumentSendRecordMapper recordMapper)
    {
        this.accessService = accessService;
        this.recordMapper = recordMapper;
    }

    /** 记录一次已经由微信确认成功的原文件发送。 */
    @Transactional
    public boolean record(Long userId, Long documentId, String requestId)
    {
        if (userId == null || userId <= 0) throw new ServiceException("微信用户编号不能为空");
        if (documentId == null || documentId <= 0) throw new ServiceException("文档编号不正确");
        String normalizedRequestId = requestId == null ? "" : requestId.trim();
        if (normalizedRequestId.isEmpty()) throw new ServiceException("发送请求号不能为空");
        if (normalizedRequestId.length() > 64)
            throw new ServiceException("发送请求号不能超过64个字符");

        WlDocumentSendRecord existing = recordMapper.selectByRequestId(normalizedRequestId);
        if (existing != null) return requireMatching(existing, userId, documentId);
        accessService.validateOriginalFileSendPermission(userId, documentId);

        WlDocumentSendRecord record = new WlDocumentSendRecord();
        record.setUserId(userId);
        record.setDocumentId(documentId);
        record.setRequestId(normalizedRequestId);
        record.setSendTime(new Date());
        record.setCreateBy("wx:" + userId);
        if (recordMapper.insertRecord(record) == 1) return true;

        existing = recordMapper.selectByRequestIdForUpdate(normalizedRequestId);
        if (existing != null) return requireMatching(existing, userId, documentId);
        throw new ServiceException("发送记录失败，请重试");
    }

    private boolean requireMatching(WlDocumentSendRecord existing, Long userId, Long documentId)
    {
        if (!userId.equals(existing.getUserId()) || !documentId.equals(existing.getDocumentId()))
            throw new ServiceException("发送请求号已被使用");
        return true;
    }
}
