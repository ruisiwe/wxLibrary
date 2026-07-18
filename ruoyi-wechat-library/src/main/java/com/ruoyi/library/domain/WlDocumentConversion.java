package com.ruoyi.library.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import java.util.Date;

/** 文档转换持久化任务。 */
public class WlDocumentConversion extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long documentId;
    private Integer taskVersion;
    private String taskStatus;
    private String sourceObjectKey;
    private String fullObjectKey;
    private String previewObjectKey;
    private Integer pageCount;
    private String failureReason;
    private Date startedTime;
    private Date finishedTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Integer getTaskVersion() { return taskVersion; }
    public void setTaskVersion(Integer taskVersion) { this.taskVersion = taskVersion; }
    public String getTaskStatus() { return taskStatus; }
    public void setTaskStatus(String taskStatus) { this.taskStatus = taskStatus; }
    public String getSourceObjectKey() { return sourceObjectKey; }
    public void setSourceObjectKey(String sourceObjectKey) { this.sourceObjectKey = sourceObjectKey; }
    public String getFullObjectKey() { return fullObjectKey; }
    public void setFullObjectKey(String fullObjectKey) { this.fullObjectKey = fullObjectKey; }
    public String getPreviewObjectKey() { return previewObjectKey; }
    public void setPreviewObjectKey(String previewObjectKey) { this.previewObjectKey = previewObjectKey; }
    public Integer getPageCount() { return pageCount; }
    public void setPageCount(Integer pageCount) { this.pageCount = pageCount; }
    public String getFailureReason() { return failureReason; }
    public void setFailureReason(String failureReason) { this.failureReason = failureReason; }
    public Date getStartedTime() { return startedTime; }
    public void setStartedTime(Date startedTime) { this.startedTime = startedTime; }
    public Date getFinishedTime() { return finishedTime; }
    public void setFinishedTime(Date finishedTime) { this.finishedTime = finishedTime; }
}
