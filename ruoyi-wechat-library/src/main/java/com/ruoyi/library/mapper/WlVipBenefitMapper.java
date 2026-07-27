package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlVipBenefit;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** VIP 权益介绍数据访问。 */
public interface WlVipBenefitMapper
{
    WlVipBenefit selectById(@Param("id") Long id);
    List<WlVipBenefit> selectList(WlVipBenefit query);
    List<WlVipBenefit> selectEnabled();
    int insertBenefit(WlVipBenefit benefit);
    int updateBenefit(WlVipBenefit benefit);
    int deleteBenefit(@Param("id") Long id, @Param("operator") String operator);
}
