package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlDocument;
import com.ruoyi.library.dto.DocumentOptionDto;
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
    long countBannerDocumentOptions(@Param("keyword") String keyword);
    List<DocumentOptionDto> selectBannerDocumentOptions(@Param("keyword") String keyword,
            @Param("offset") long offset, @Param("limit") int limit);
    WlDocument selectDocumentById(@Param("id") Long id);
    List<WlDocument> selectDocumentsForUpdate(@Param("ids") Long[] ids);
    List<WlDocument> selectDocumentList(WlDocument query);
    int countDocumentsByCategoryIds(@Param("categoryIds") Long[] categoryIds);
    int insertDocument(WlDocument document);
    int updateDocument(WlDocument document);
    int updateDocumentCover(@Param("id") Long id, @Param("oldCoverUrl") String oldCoverUrl,
            @Param("newCoverUrl") String newCoverUrl, @Param("operator") String operator);
    int updatePublishStatus(@Param("id") Long id, @Param("publishStatus") String publishStatus,
            @Param("operator") String operator);
    int updateConversionPending(@Param("id") Long id, @Param("originalObjectKey") String originalObjectKey,
            @Param("fileFormat") String fileFormat, @Param("fileSize") Long fileSize,
            @Param("operator") String operator);
    int updateConversionStarted(@Param("id") Long id, @Param("operator") String operator);
    int updateConversionSuccess(@Param("id") Long id, @Param("fullObjectKey") String fullObjectKey,
            @Param("previewObjectKey") String previewObjectKey, @Param("pageCount") Integer pageCount,
            @Param("operator") String operator);
    int updateConversionFailed(@Param("id") Long id, @Param("operator") String operator);
    int deleteDocuments(@Param("ids") Long[] ids, @Param("operator") String operator);
}
