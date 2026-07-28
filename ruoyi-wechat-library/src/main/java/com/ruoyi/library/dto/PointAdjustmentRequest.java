package com.ruoyi.library.dto;

/** 管理员人工调整积分请求。 */
public class PointAdjustmentRequest
{
    private Long amount;
    private String batchNo;
    private String description;

    public Long getAmount() { return amount; }
    public void setAmount(Long amount) { this.amount = amount; }
    public String getBatchNo() { return batchNo; }
    public void setBatchNo(String batchNo) { this.batchNo = batchNo; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
}
