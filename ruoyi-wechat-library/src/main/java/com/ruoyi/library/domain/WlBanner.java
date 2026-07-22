package com.ruoyi.library.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/** 首页宣传图片。 */
public class WlBanner extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private String title;
    private String imageUrl;
    private Long documentId;
    private String documentTitle;
    private String documentCategoryName;
    private String documentFileFormat;
    private Boolean documentSelectable;
    private Integer sortOrder;
    private String status;
    private Date startTime;
    private Date endTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public String getDocumentTitle() { return documentTitle; }
    public void setDocumentTitle(String documentTitle) { this.documentTitle = documentTitle; }
    public String getDocumentCategoryName() { return documentCategoryName; }
    public void setDocumentCategoryName(String documentCategoryName) { this.documentCategoryName = documentCategoryName; }
    public String getDocumentFileFormat() { return documentFileFormat; }
    public void setDocumentFileFormat(String documentFileFormat) { this.documentFileFormat = documentFileFormat; }
    public Boolean getDocumentSelectable() { return documentSelectable; }
    public void setDocumentSelectable(Boolean documentSelectable) { this.documentSelectable = documentSelectable; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Date getStartTime() { return startTime; }
    public void setStartTime(Date startTime) { this.startTime = startTime; }
    public Date getEndTime() { return endTime; }
    public void setEndTime(Date endTime) { this.endTime = endTime; }
}
