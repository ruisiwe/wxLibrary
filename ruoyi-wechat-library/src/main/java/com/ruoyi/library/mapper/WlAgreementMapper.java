package com.ruoyi.library.mapper;

import java.util.List;
import com.ruoyi.library.domain.WlAgreement;
import org.apache.ibatis.annotations.Param;

/** 协议版本数据访问。 */
public interface WlAgreementMapper
{
    WlAgreement selectCurrentByType(@Param("agreementType") String agreementType);
    WlAgreement selectAgreementById(@Param("id") Long id);
    WlAgreement selectAgreementByIdForUpdate(@Param("id") Long id);
    List<WlAgreement> selectAgreementList(WlAgreement agreement);
    int insertAgreement(WlAgreement agreement);
    int updateDraft(WlAgreement agreement);
    int disablePublishedByType(@Param("agreementType") String agreementType,
            @Param("excludeId") Long excludeId, @Param("operator") String operator);
    int publishAgreement(@Param("id") Long id, @Param("operator") String operator);
}
