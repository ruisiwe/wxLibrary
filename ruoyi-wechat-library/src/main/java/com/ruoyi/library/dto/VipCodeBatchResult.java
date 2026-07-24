package com.ruoyi.library.dto;

import java.util.Collections;
import java.util.List;

/** 会员码批量生成结果，明文只用于本次响应。 */
public class VipCodeBatchResult
{
    private final String batchNo;
    private final List<String> plaintextCodes;

    public VipCodeBatchResult(String batchNo, List<String> plaintextCodes)
    {
        this.batchNo = batchNo;
        this.plaintextCodes = Collections.unmodifiableList(plaintextCodes);
    }

    public String getBatchNo() { return batchNo; }
    public List<String> getPlaintextCodes() { return plaintextCodes; }
}
