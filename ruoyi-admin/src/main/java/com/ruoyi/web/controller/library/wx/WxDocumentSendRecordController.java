package com.ruoyi.web.controller.library.wx;

import java.util.Collections;
import java.util.Map;
import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.dto.DocumentSendRecordRequest;
import com.ruoyi.library.service.DocumentSendRecordService;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序文档发送成功记录接口。 */
@RestController
@RequestMapping("/wx/documents")
public class WxDocumentSendRecordController
{
    private final DocumentSendRecordService service;

    public WxDocumentSendRecordController(DocumentSendRecordService service)
    {
        this.service = service;
    }

    /** 记录当前微信用户成功发送一次文档原文件。 */
    @PostMapping("/{id}/send-record")
    public WxApiResponse<Map<String, Boolean>> record(@PathVariable Long id,
            @RequestBody DocumentSendRecordRequest request)
    {
        service.record(WxUserContext.get(), id, request == null ? null : request.getRequestId());
        return WxApiResponse.success(Collections.singletonMap("recorded", true));
    }
}
