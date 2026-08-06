package com.ruoyi.library.dto;

/** 文档兑换请求。 */
public class DocumentUnlockRequest
{
    private String requestId;
    private Boolean freeOnly;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public boolean isFreeOnly() { return Boolean.TRUE.equals(freeOnly); }
    public void setFreeOnly(Boolean freeOnly) { this.freeOnly = freeOnly; }
}
