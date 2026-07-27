package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlUserAgreement;
import org.apache.ibatis.annotations.Param;

/** 用户协议确认数据访问。 */
public interface WlUserAgreementMapper
{
    WlUserAgreement selectByUserAndAgreement(@Param("userId") Long userId,
            @Param("agreementId") Long agreementId);
    int countAcceptedAgreementId(@Param("userId") Long userId, @Param("agreementId") Long agreementId);
    int insertUserAgreement(WlUserAgreement agreement);
}
