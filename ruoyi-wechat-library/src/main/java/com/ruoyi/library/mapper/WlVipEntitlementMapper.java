package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlVipEntitlement;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 会员权益台账数据访问。 */
public interface WlVipEntitlementMapper
{
    WlVipEntitlement selectBySource(@Param("sourceType") String sourceType,
            @Param("sourceBizNo") String sourceBizNo);
    List<WlVipEntitlement> selectList(WlVipEntitlement query);
    int insertEntitlement(WlVipEntitlement entitlement);
    int revokeBySource(@Param("sourceType") String sourceType, @Param("sourceBizNo") String sourceBizNo,
            @Param("operator") String operator);
    List<WlVipEntitlement> selectActiveAfterId(@Param("userId") Long userId, @Param("id") Long id);
    int revokeById(@Param("id") Long id, @Param("operator") String operator);
}
