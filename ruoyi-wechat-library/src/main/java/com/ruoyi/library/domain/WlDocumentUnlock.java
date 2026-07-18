package com.ruoyi.library.domain;

import java.util.Date;
import com.ruoyi.common.core.domain.BaseEntity;

/** 微信用户文档兑换权限。 */
public class WlDocumentUnlock extends BaseEntity
{
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long userId;
    private Long documentId;
    private Long spentPoints;
    private Long pointRecordId;
    private Date unlockTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getDocumentId() { return documentId; }
    public void setDocumentId(Long documentId) { this.documentId = documentId; }
    public Long getSpentPoints() { return spentPoints; }
    public void setSpentPoints(Long spentPoints) { this.spentPoints = spentPoints; }
    public Long getPointRecordId() { return pointRecordId; }
    public void setPointRecordId(Long pointRecordId) { this.pointRecordId = pointRecordId; }
    public Date getUnlockTime() { return unlockTime; }
    public void setUnlockTime(Date unlockTime) { this.unlockTime = unlockTime; }
}
