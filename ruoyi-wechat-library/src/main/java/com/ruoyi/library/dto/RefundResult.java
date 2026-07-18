package com.ruoyi.library.dto;
/** 退款完成后的积分追回结果。 */
public class RefundResult
{
    private final long recoveredPoints; private final long unrecoveredPoints;
    public RefundResult(long recoveredPoints,long unrecoveredPoints){this.recoveredPoints=recoveredPoints;this.unrecoveredPoints=unrecoveredPoints;}
    public long getRecoveredPoints(){return recoveredPoints;} public long getUnrecoveredPoints(){return unrecoveredPoints;}
}
