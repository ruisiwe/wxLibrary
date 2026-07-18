package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.dto.DocumentSummaryDto;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 文档数据访问。 */
public interface WlDocumentMapper
{
    List<DocumentSummaryDto> selectPublishedDocuments(@Param("keyword") String keyword,
            @Param("categoryId") Long categoryId, @Param("offset") long offset, @Param("limit") int limit);
    long countPublishedDocuments(@Param("keyword") String keyword, @Param("categoryId") Long categoryId);
    DocumentSummaryDto selectPublishedDocumentById(@Param("id") Long id);
    WlDocument selectDocumentById(@Param("id") Long id);
    List<WlDocument> selectDocumentList(WlDocument query);
    int countDocumentsByCategoryIds(@Param("categoryIds") Long[] categoryIds);
    int insertDocument(WlDocument document);
    int updateDocument(WlDocument document);
    int updatePublishStatus(@Param("id") Long id, @Param("publishStatus") String publishStatus,
            @Param("operator") String operator);
    int deleteDocuments(@Param("ids") Long[] ids, @Param("operator") String operator);
}
