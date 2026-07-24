package com.ruoyi.library.dto;

/** 后台文档上传会话确认保存参数。 */
public class DocumentUploadCommitRequest
{
    private Long categoryId;
    private String title;
    private String summary;
    private String tags;
    private Long pointPrice;
    private String accessType;
    private Integer sortOrder;
    private String remark;

    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public Long getPointPrice() { return pointPrice; }
    public void setPointPrice(Long pointPrice) { this.pointPrice = pointPrice; }
    public String getAccessType() { return accessType; }
    public void setAccessType(String accessType) { this.accessType = accessType; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
}
