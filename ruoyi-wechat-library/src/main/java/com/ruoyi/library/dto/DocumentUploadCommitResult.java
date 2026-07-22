package com.ruoyi.library.dto;

/** 后台文档上传会话保存结果。 */
public class DocumentUploadCommitResult
{
    private Long documentId;
    private String conversionStatus;
    private String thumbnailUrl;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getConversionStatus() { return conversionStatus; }
    public void setConversionStatus(String conversionStatus) { this.conversionStatus = conversionStatus; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
}
