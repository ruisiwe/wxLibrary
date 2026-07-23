package com.ruoyi.library.dto;

/** 宣传图片关联文档搜索选项。 */
public class DocumentOptionDto
{
    private Long id;
    private String title;
    private String categoryName;
    private String fileFormat;
    private Boolean documentSelectable;
    private String availabilityStatus;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public Boolean getDocumentSelectable() { return documentSelectable; }
    public void setDocumentSelectable(Boolean documentSelectable) { this.documentSelectable = documentSelectable; }
    public String getAvailabilityStatus() { return availabilityStatus; }
    public void setAvailabilityStatus(String availabilityStatus) { this.availabilityStatus = availabilityStatus; }
}
