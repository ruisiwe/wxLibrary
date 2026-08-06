package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlDocumentSendRecord;
import org.apache.ibatis.annotations.Param;

/** 文档发送成功记录数据访问。 */
public interface WlDocumentSendRecordMapper
{
    WlDocumentSendRecord selectByRequestId(@Param("requestId") String requestId);
    WlDocumentSendRecord selectByRequestIdForUpdate(@Param("requestId") String requestId);
    int insertRecord(WlDocumentSendRecord record);
}
