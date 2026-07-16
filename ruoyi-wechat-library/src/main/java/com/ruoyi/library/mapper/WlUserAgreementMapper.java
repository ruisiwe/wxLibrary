package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlUserAgreement;
import org.apache.ibatis.annotations.Param;

/** 用户协议确认数据访问。 */
public interface WlUserAgreementMapper
{
    WlUserAgreement selectByUserAndAgreement(@Param("userId") Long userId,
            @Param("agreementId") Long agreementId);
    int countAcceptedAgreementIds(@Param("userId") Long userId,
            @Param("privacyId") Long privacyId, @Param("statementId") Long statementId);
    int insertUserAgreement(WlUserAgreement agreement);
}
