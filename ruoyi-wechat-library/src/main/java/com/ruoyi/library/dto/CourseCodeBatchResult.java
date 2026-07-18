package com.ruoyi.library.dto;
import java.util.Collections;import java.util.List;
/** 兑换码一次性生成结果，明文仅在本对象中返回。 */
public class CourseCodeBatchResult
{private final String batchNo;private final List<String> plaintextCodes;public CourseCodeBatchResult(String b,List<String> c){batchNo=b;plaintextCodes=Collections.unmodifiableList(c);}public String getBatchNo(){return batchNo;}public List<String> getPlaintextCodes(){return plaintextCodes;}}
