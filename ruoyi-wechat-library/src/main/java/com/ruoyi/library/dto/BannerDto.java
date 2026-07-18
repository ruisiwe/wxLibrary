package com.ruoyi.library.dto;

/** 小程序首页宣传图片。 */
public class BannerDto
{
    private Long id;
    private String title;
    private String imageUrl;
    private Long documentId;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
}
