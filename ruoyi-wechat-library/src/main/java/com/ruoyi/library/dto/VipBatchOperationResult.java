package com.ruoyi.library.dto;

/** VIP 批量操作结果。 */
public final class VipBatchOperationResult
{
    private final int processedCount;

    public VipBatchOperationResult(int processedCount)
    {
        this.processedCount = processedCount;
    }

    public int getProcessedCount()
    {
        return processedCount;
    }
}
