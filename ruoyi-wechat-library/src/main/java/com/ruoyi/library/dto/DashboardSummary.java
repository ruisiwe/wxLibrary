package com.ruoyi.library.dto;

/** 后台首页顶部四项累计指标。 */
public class DashboardSummary
{
    private Long userCount;
    private Long memberCount;
    private Long documentCount;
    private Long paidDocumentCount;

    public DashboardSummary() { }

    public DashboardSummary(Long userCount, Long memberCount, Long documentCount,
            Long paidDocumentCount)
    {
        this.userCount = userCount;
        this.memberCount = memberCount;
        this.documentCount = documentCount;
        this.paidDocumentCount = paidDocumentCount;
    }

    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }
    public Long getMemberCount() { return memberCount; }
    public void setMemberCount(Long memberCount) { this.memberCount = memberCount; }
    public Long getDocumentCount() { return documentCount; }
    public void setDocumentCount(Long documentCount) { this.documentCount = documentCount; }
    public Long getPaidDocumentCount() { return paidDocumentCount; }
    public void setPaidDocumentCount(Long paidDocumentCount) { this.paidDocumentCount = paidDocumentCount; }
}
