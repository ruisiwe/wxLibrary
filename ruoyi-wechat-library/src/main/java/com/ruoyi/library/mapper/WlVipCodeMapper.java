package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlVipCode;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 会员兑换码数据访问。 */
public interface WlVipCodeMapper
{
    WlVipCode selectByDigest(@Param("digest") String digest);
    WlVipCode selectByDigestForUpdate(@Param("digest") String digest);
    List<WlVipCode> selectList(WlVipCode query);
    int insertCode(WlVipCode code);
    int markUsed(@Param("id") Long id, @Param("userId") Long userId,
            @Param("vipEntitlementId") Long vipEntitlementId);
    int disable(@Param("id") Long id, @Param("operator") String operator);
}
