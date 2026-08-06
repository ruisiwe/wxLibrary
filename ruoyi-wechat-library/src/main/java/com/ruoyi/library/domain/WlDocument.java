package com.ruoyi.library.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/** 文档元数据。 */
public class WlDocument extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long categoryId;
    private String categoryName;
    private String title;
    private String summary;
    private String tags;
    private String coverUrl;
    private String uploaderName;
    private String fileFormat;
    private Long fileSize;
    private Integer pageCount;
    private Long pointPrice;
    private String accessType;
    private Integer previewPages;
    private String originalObjectKey;
    private String fullObjectKey;
    private String previewObjectKey;
    private String conversionStatus;
    private String publishStatus;
    private Date publishTime;
    private Long viewCount;
    private Integer sortOrder;

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
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getCoverUrl() { return coverUrl; }
    public void setCoverUrl(String coverUrl) { this.coverUrl = coverUrl; }
    public String getUploaderName() { return uploaderName; }
    public void setUploaderName(String uploaderName) { this.uploaderName = uploaderName; }
    public String getFileFormat() { return fileFormat; }
    public void setFileFormat(String fileFormat) { this.fileFormat = fileFormat; }
    public Long getFileSize() { return fileSize; }
    public void setFileSize(Long fileSize) { this.fileSize = fileSize; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public Long getPointPrice() { return pointPrice; }
    public void setPointPrice(Long pointPrice) { this.pointPrice = pointPrice; }
    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }
    public Integer getPreviewPages() { return previewPages; }
    public void setPreviewPages(Integer previewPages) { this.previewPages = previewPages; }
    public String getOriginalObjectKey() { return originalObjectKey; }
    public void setOriginalObjectKey(String originalObjectKey) { this.originalObjectKey = originalObjectKey; }
    public String getFullObjectKey() { return fullObjectKey; }
    public void setFullObjectKey(String fullObjectKey) { this.fullObjectKey = fullObjectKey; }
    public String getPreviewObjectKey() { return previewObjectKey; }
    public void setPreviewObjectKey(String previewObjectKey) { this.previewObjectKey = previewObjectKey; }
    public String getConversionStatus() { return conversionStatus; }
    public void setConversionStatus(String conversionStatus) { this.conversionStatus = conversionStatus; }
    public String getPublishStatus() { return publishStatus; }
    public void setPublishStatus(String publishStatus) { this.publishStatus = publishStatus; }
    public Date getPublishTime() { return publishTime; }
    public void setPublishTime(Date publishTime) { this.publishTime = publishTime; }
    public Long getViewCount() { return viewCount; }
    public void setViewCount(Long viewCount) { this.viewCount = viewCount; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
}
