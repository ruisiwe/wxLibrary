package com.ruoyi.library.dto;

/** 公开文档元数据，不包含任何私有文件对象键。 */
public class DocumentSummaryDto
{
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String summary;
    private String coverUrl;
    private String uploaderName;
    private Long viewCount;
    private String fileFormat;
    private Integer pageCount;
    private Long pointPrice;
    private String accessType;
    private String accessLabel;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public Long getPointPrice() { return pointPrice; }
    public void setPointPrice(Long pointPrice) { this.pointPrice = pointPrice; }
    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }
    public String getAccessLabel() { return accessLabel; }
    public void setAccessLabel(String accessLabel) { this.accessLabel = accessLabel; }
}
