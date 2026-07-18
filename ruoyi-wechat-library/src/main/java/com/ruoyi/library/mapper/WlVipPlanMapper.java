package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlVipPlan;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 会员套餐数据访问。 */
public interface WlVipPlanMapper
{
    WlVipPlan selectById(@Param("id") Long id);
    WlVipPlan selectEnabledById(@Param("id") Long id);
    List<WlVipPlan> selectList(WlVipPlan query);
    int insertPlan(WlVipPlan plan);
    int updatePlan(WlVipPlan plan);
    int deletePlan(@Param("id") Long id, @Param("operator") String operator);
}
