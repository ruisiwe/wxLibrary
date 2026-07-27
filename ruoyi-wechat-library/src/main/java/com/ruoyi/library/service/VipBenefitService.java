package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipBenefit;
import com.ruoyi.library.mapper.WlVipBenefitMapper;
import java.util.List;
import org.springframework.stereotype.Service;

/** VIP 权益介绍维护服务。 */
@Service
public class VipBenefitService
{
    private final WlVipBenefitMapper benefitMapper;

    public VipBenefitService(WlVipBenefitMapper benefitMapper)
    {
        this.benefitMapper = benefitMapper;
    }

    public List<WlVipBenefit> list(WlVipBenefit query)
    {
        return benefitMapper.selectList(query == null ? new WlVipBenefit() : query);
    }

    public List<WlVipBenefit> listEnabled()
    {
        return benefitMapper.selectEnabled();
    }

    public WlVipBenefit get(Long id)
    {
        requireId(id);
        WlVipBenefit benefit = benefitMapper.selectById(id);
        if (benefit == null) throw new ServiceException("VIP 权益不存在");
        return benefit;
    }

    public int add(WlVipBenefit benefit, String operator)
    {
        validate(benefit, false);
        benefit.setCreateBy(requireOperator(operator));
        return benefitMapper.insertBenefit(benefit);
    }

    public int edit(WlVipBenefit benefit, String operator)
    {
        validate(benefit, true);
        get(benefit.getId());
        benefit.setUpdateBy(requireOperator(operator));
        return benefitMapper.updateBenefit(benefit);
    }

    public int remove(Long id, String operator)
    {
        get(id);
        return benefitMapper.deleteBenefit(id, requireOperator(operator));
    }

    private void validate(WlVipBenefit benefit, boolean requireId)
    {
        if (benefit == null) throw new ServiceException("权益参数不能为空");
        if (requireId) requireId(benefit.getId());
        if (benefit.getBenefitText() == null || benefit.getBenefitText().trim().isEmpty())
            throw new ServiceException("权益文字不能为空");
        String text = benefit.getBenefitText().trim();
        if (text.length() > 100) throw new ServiceException("权益文字不能超过100个字符");
        if (benefit.getSortOrder() == null) benefit.setSortOrder(0);
        if (benefit.getSortOrder() < 0) throw new ServiceException("权益排序不能小于0");
        if (!"0".equals(benefit.getStatus()) && !"1".equals(benefit.getStatus()))
            throw new ServiceException("权益状态不正确");
        benefit.setBenefitText(text);
    }

    private void requireId(Long id)
    {
        if (id == null || id <= 0) throw new ServiceException("权益编号不能为空");
    }

    private String requireOperator(String operator)
    {
        if (operator == null || operator.trim().isEmpty()) throw new ServiceException("操作人不能为空");
        return operator.trim();
    }
}
