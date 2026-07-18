package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlDocumentUnlock;
import com.ruoyi.library.dto.DocumentSummaryDto;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 文档兑换、收藏和访问记录数据访问。 */
public interface WlDocumentUnlockMapper
{
    WlDocumentUnlock selectUnlock(@Param("userId") Long userId, @Param("documentId") Long documentId);
    int insertUnlock(WlDocumentUnlock unlock);
    int saveFavorite(@Param("userId") Long userId, @Param("documentId") Long documentId);
    int deleteFavorite(@Param("userId") Long userId, @Param("documentId") Long documentId);
    int countFavorite(@Param("userId") Long userId, @Param("documentId") Long documentId);
    List<DocumentSummaryDto> selectUnlockedDocuments(@Param("userId") Long userId);
    List<DocumentSummaryDto> selectFavoriteDocuments(@Param("userId") Long userId);
    int insertView(@Param("userId") Long userId, @Param("documentId") Long documentId,
            @Param("viewType") String viewType, @Param("clientIp") String clientIp);
    int incrementViewCount(@Param("documentId") Long documentId);
}
