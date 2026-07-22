package com.ruoyi.library.dto;

/** 已保存文档缩略图替换结果。 */
public class DocumentThumbnailResult
{
    private Long documentId;
    private String thumbnailUrl;

    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getThumbnailUrl() { return thumbnailUrl; }
    public void setThumbnailUrl(String thumbnailUrl) { this.thumbnailUrl = thumbnailUrl; }
}
