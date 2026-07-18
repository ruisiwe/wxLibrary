package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlDocumentConversion;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 文档转换任务数据访问。 */
public interface WlDocumentConversionMapper
{
    WlDocumentConversion selectById(@Param("id") Long id);
    List<WlDocumentConversion> selectList(WlDocumentConversion query);
    int selectNextVersion(@Param("documentId") Long documentId);
    int insertConversion(WlDocumentConversion conversion);
    int markConverting(@Param("id") Long id);
    int markSuccess(@Param("id") Long id, @Param("fullObjectKey") String fullObjectKey,
            @Param("previewObjectKey") String previewObjectKey, @Param("pageCount") Integer pageCount);
    int markFailed(@Param("id") Long id, @Param("failureReason") String failureReason);
}
