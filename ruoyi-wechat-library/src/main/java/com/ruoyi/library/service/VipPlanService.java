package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.mapper.WlVipPlanMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/** 会员套餐配置服务。 */
@Service
public class VipPlanService
{
    private static final int MAX_VALID_DAYS = 3650;
    private final WlVipPlanMapper planMapper;

    public VipPlanService(WlVipPlanMapper planMapper) { this.planMapper = planMapper; }

    public List<WlVipPlan> list(WlVipPlan query)
    {
        return planMapper.selectList(query == null ? new WlVipPlan() : query);
    }

    public WlVipPlan get(Long id)
    {
        WlVipPlan plan = planMapper.selectById(id);
        if (plan == null) throw new ServiceException("会员套餐不存在");
        return plan;
    }

    public WlVipPlan getEnabled(Long id)
    {
        WlVipPlan plan = planMapper.selectEnabledById(id);
        if (plan == null) throw new ServiceException("会员套餐不存在或已停用");
        return plan;
    }

    public int add(WlVipPlan plan, String operator)
    {
        validate(plan, false);
        plan.setCreateBy(operator);
        return planMapper.insertPlan(plan);
    }

    public int edit(WlVipPlan plan, String operator)
    {
        validate(plan, true);
        get(plan.getId());
        plan.setUpdateBy(operator);
        return planMapper.updatePlan(plan);
    }

    public int remove(Long id, String operator)
    {
        get(id);
        return planMapper.deletePlan(id, operator);
    }

    private void validate(WlVipPlan plan, boolean requireId)
    {
        if (plan == null || requireId && plan.getId() == null) throw new ServiceException("会员套餐编号不能为空");
        if (plan.getPlanCode() == null || plan.getPlanCode().trim().isEmpty()) throw new ServiceException("套餐编码不能为空");
        if (plan.getPlanCode().trim().length() > 32) throw new ServiceException("套餐编码不能超过32个字符");
        if (plan.getPlanName() == null || plan.getPlanName().trim().isEmpty()) throw new ServiceException("套餐名称不能为空");
        if (plan.getPriceCent() == null || plan.getPriceCent() < 0) throw new ServiceException("套餐价格不能小于0");
        if (plan.getValidDays() == null || plan.getValidDays() < 1
                || plan.getValidDays() > MAX_VALID_DAYS)
            throw new ServiceException("会员套餐有效天数必须在1到3650天之间");
        if (plan.getGiftPoints() == null || plan.getGiftPoints() < 0) throw new ServiceException("赠送积分不能小于0");
        if (!"0".equals(plan.getStatus()) && !"1".equals(plan.getStatus())) throw new ServiceException("套餐状态不正确");
        if (plan.getSortOrder() == null) plan.setSortOrder(0);
        plan.setPlanCode(plan.getPlanCode().trim());
        plan.setPlanName(plan.getPlanName().trim());
    }
}
