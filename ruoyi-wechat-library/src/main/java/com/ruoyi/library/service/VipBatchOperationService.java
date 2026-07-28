package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.VipBatchOperationResult;
import com.ruoyi.library.dto.VipOperationRequest;
import com.ruoyi.library.mapper.WlWxUserMapper;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeSet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 后台 VIP 批量开通、续期和补偿服务。 */
@Service
public class VipBatchOperationService
{
    private static final int MAX_USERS = 100;
    private static final String BATCH_PATTERN = "[A-Za-z0-9]{20}";

    private final VipPlanService planService;
    private final VipEntitlementService entitlementService;
    private final WlWxUserMapper userMapper;

    public VipBatchOperationService(VipPlanService planService, VipEntitlementService entitlementService,
            WlWxUserMapper userMapper)
    {
        this.planService = planService;
        this.entitlementService = entitlementService;
        this.userMapper = userMapper;
    }

    @Transactional
    public VipBatchOperationResult open(VipOperationRequest request, Long operatorId)
    {
        List<Long> userIds = normalize(request);
        if (request.getPlanId() == null) throw new ServiceException("会员套餐编号不能为空");
        WlVipPlan plan = planService.getEnabled(request.getPlanId());
        String reason = request.getReason().trim();
        for (Long userId : userIds)
        {
            entitlementService.openOrRenew(userId, plan, "MANUAL",
                    businessNo("MANUAL", request.getBatchNo(), userId), operatorId, reason);
        }
        return new VipBatchOperationResult(userIds.size());
    }

    @Transactional
    public VipBatchOperationResult compensate(VipOperationRequest request, Long operatorId)
    {
        List<Long> userIds = normalize(request);
        if (request.getDays() == null || request.getDays() <= 0)
        {
            throw new ServiceException("补偿天数必须大于0");
        }
        String reason = request.getReason().trim();
        for (Long userId : userIds)
        {
            entitlementService.compensate(userId, request.getDays(), operatorId, reason,
                    businessNo("COMPENSATION", request.getBatchNo(), userId));
        }
        return new VipBatchOperationResult(userIds.size());
    }

    public List<WlWxUser> userOptions(String keyword)
    {
        String normalized = keyword == null ? null : keyword.trim();
        if (normalized != null && normalized.isEmpty()) normalized = null;
        Long userId = parseUserId(normalized);
        return userMapper.selectVipOperationCandidates(normalized, userId);
    }

    private List<Long> normalize(VipOperationRequest request)
    {
        if (request == null) throw new ServiceException("会员操作请求不能为空");
        if (request.getUserIds() == null || request.getUserIds().isEmpty())
        {
            throw new ServiceException("请选择微信用户");
        }
        TreeSet<Long> unique = new TreeSet<>();
        for (Long userId : request.getUserIds())
        {
            if (userId == null || userId <= 0) throw new ServiceException("微信用户编号不正确");
            unique.add(userId);
        }
        if (unique.size() > MAX_USERS) throw new ServiceException("一次最多选择100位用户");
        if (request.getBatchNo() == null || !request.getBatchNo().matches(BATCH_PATTERN))
        {
            throw new ServiceException("批次编号不正确");
        }
        if (request.getReason() == null || request.getReason().trim().isEmpty())
        {
            throw new ServiceException("操作原因不能为空");
        }
        if (request.getReason().trim().length() > 500)
        {
            throw new ServiceException("操作原因不能超过500个字符");
        }
        return new ArrayList<>(unique);
    }

    private String businessNo(String sourceType, String batchNo, Long userId)
    {
        return sourceType + ":" + batchNo + ":" + userId;
    }

    private Long parseUserId(String keyword)
    {
        if (keyword == null || !keyword.matches("\\d+")) return null;
        try
        {
            long value = Long.parseLong(keyword);
            return value > 0 ? value : null;
        }
        catch (NumberFormatException ignored)
        {
            return null;
        }
    }
}
