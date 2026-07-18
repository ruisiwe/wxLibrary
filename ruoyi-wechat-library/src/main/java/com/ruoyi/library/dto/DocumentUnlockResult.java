package com.ruoyi.library.dto;

/** 文档兑换结果。 */
public class DocumentUnlockResult
{
    private final Long documentId;
    private final boolean unlocked;
    private final Long spentPoints;
    private final Long pointBalance;

    public DocumentUnlockResult(Long documentId, boolean unlocked, Long spentPoints, Long pointBalance)
    {
        this.documentId = documentId;
        this.unlocked = unlocked;
        this.spentPoints = spentPoints;
        this.pointBalance = pointBalance;
    }

    public Long getDocumentId() { return documentId; }
    public boolean isUnlocked() { return unlocked; }
    public Long getSpentPoints() { return spentPoints; }
    public Long getPointBalance() { return pointBalance; }
}
