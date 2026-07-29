package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.domain.WlVipEntitlement;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.mapper.WlVipEntitlementMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 会员开通、续期、补偿和权益台账服务。 */
@Service
public class VipEntitlementService
{
    private static final Set<String> PLAN_SOURCES = new HashSet<>(Arrays.asList("PAYMENT", "MANUAL", "VIP_CODE"));
    private static final int MAX_PLAN_VALID_DAYS = 3650;
    private final WlVipEntitlementMapper entitlementMapper;
    private final WlWxUserMapper userMapper;
    private final PointService pointService;
    private final Clock clock;

    @Autowired
    public VipEntitlementService(WlVipEntitlementMapper entitlementMapper, WlWxUserMapper userMapper,
            PointService pointService)
    {
        this(entitlementMapper, userMapper, pointService, Clock.systemUTC());
    }

    VipEntitlementService(WlVipEntitlementMapper entitlementMapper, WlWxUserMapper userMapper,
            PointService pointService, Clock clock)
    {
        this.entitlementMapper = entitlementMapper;
        this.userMapper = userMapper;
        this.pointService = pointService;
        this.clock = clock;
    }

    @Transactional
    public WlVipEntitlement openOrRenew(Long userId, WlVipPlan plan, String sourceType, String sourceBizNo)
    {
        return openOrRenew(userId, plan, sourceType, sourceBizNo, null, "");
    }

    @Transactional
    public WlVipEntitlement openOrRenew(Long userId, WlVipPlan plan, String sourceType,
            String sourceBizNo, Long operatorId, String reason)
    {
        validatePlan(plan);
        if (!PLAN_SOURCES.contains(sourceType)) throw new ServiceException("会员权益来源类型不正确");
        return grant(userId, plan.getValidDays(), plan.getGiftPoints(), sourceType, sourceBizNo,
                operatorId, reason);
    }

    @Transactional
    public WlVipEntitlement compensate(Long userId, int days, Long operatorId, String reason,
            String sourceBizNo)
    {
        return grant(userId, days, 0L, "COMPENSATION", sourceBizNo, operatorId, reason);
    }

    @Transactional
    public WlVipEntitlement compensate(Long userId, int days, Long operatorId, String reason)
    {
        String bizNo = "COMPENSATION:" + userId + ":" + operatorId + ":" + clock.millis();
        return compensate(userId, days, operatorId, reason, bizNo);
    }

    public List<WlVipEntitlement> list(WlVipEntitlement query)
    {
        return entitlementMapper.selectList(query == null ? new WlVipEntitlement() : query);
    }

    private WlVipEntitlement grant(Long userId, int days, long giftPoints, String sourceType,
            String sourceBizNo, Long operatorId, String reason)
    {
        validateGrant(userId, days, giftPoints, sourceBizNo, reason);
        WlVipEntitlement existing = entitlementMapper.selectBySource(sourceType, sourceBizNo.trim());
        if (existing != null) return requireSameUser(existing, userId);

        WlWxUser locked = requireEnabledUser(userMapper.selectByIdForUpdate(userId));
        existing = entitlementMapper.selectBySource(sourceType, sourceBizNo.trim());
        if (existing != null) return requireSameUser(existing, userId);

        Instant now = clock.instant();
        Date oldExpire = locked.getVipExpireTime();
        Instant start = oldExpire != null && oldExpire.toInstant().isAfter(now) ? oldExpire.toInstant() : now;
        Date newExpire = Date.from(start.plus(days, ChronoUnit.DAYS));
        String operator = operatorId == null ? "system" : String.valueOf(operatorId);
        if (userMapper.updateVipExpireTime(userId, newExpire, operator) != 1)
            throw new ServiceException("会员到期时间更新失败，请重试");
        locked.setVipExpireTime(newExpire);

        WlPointRecord pointRecord = giftPoints == 0L ? null : pointService.creditFixedAfterLock(
                locked, giftPoints, "VIP_GIFT:" + sourceBizNo.trim(), "会员套餐赠送积分");
        WlVipEntitlement entitlement = new WlVipEntitlement();
        entitlement.setUserId(userId);
        entitlement.setSourceType(sourceType);
        entitlement.setSourceBizNo(sourceBizNo.trim());
        entitlement.setStartTime(Date.from(start));
        entitlement.setEndTime(newExpire);
        entitlement.setGrantedDays(days);
        entitlement.setGiftPoints(giftPoints);
        entitlement.setPointRecordId(pointRecord == null ? null : pointRecord.getId());
        entitlement.setStatus("ACTIVE");
        entitlement.setOperatorId(operatorId);
        entitlement.setReason(safeReason(reason));
        entitlement.setOldExpireTime(oldExpire);
        entitlement.setNewExpireTime(newExpire);
        entitlement.setCreateBy(operator);
        if (entitlementMapper.insertEntitlement(entitlement) != 1)
            throw new ServiceException("会员权益记录失败，请重试");
        return entitlement;
    }

    private WlVipEntitlement requireSameUser(WlVipEntitlement existing, Long userId)
    {
        if (!userId.equals(existing.getUserId())) throw new ServiceException("会员业务编号已被其他用户使用");
        return existing;
    }

    private WlWxUser requireEnabledUser(WlWxUser user)
    {
        if (user == null) throw new ServiceException("微信用户不存在");
        if (!"0".equals(user.getStatus())) throw new ServiceException("当前账号已停用，请联系管理员");
        return user;
    }

    private void validatePlan(WlVipPlan plan)
    {
        if (plan == null || plan.getId() == null) throw new ServiceException("会员套餐不存在");
        if (!"0".equals(plan.getStatus())) throw new ServiceException("会员套餐已停用");
        if (plan.getValidDays() == null || plan.getValidDays() < 1
                || plan.getValidDays() > MAX_PLAN_VALID_DAYS)
            throw new ServiceException("会员套餐有效天数必须在1到3650天之间");
        if (plan.getGiftPoints() == null || plan.getGiftPoints() < 0)
            throw new ServiceException("会员套餐赠送积分不正确");
    }

    private void validateGrant(Long userId, int days, long giftPoints, String sourceBizNo, String reason)
    {
        if (userId == null || userId <= 0) throw new ServiceException("微信用户编号不正确");
        if (days <= 0) throw new ServiceException("会员有效天数必须大于0");
        if (giftPoints < 0) throw new ServiceException("赠送积分不能小于0");
        if (sourceBizNo == null || sourceBizNo.trim().isEmpty()) throw new ServiceException("会员业务编号不能为空");
        if (sourceBizNo.trim().length() > 64) throw new ServiceException("会员业务编号不能超过64个字符");
        if (reason != null && reason.trim().length() > 500) throw new ServiceException("操作原因不能超过500个字符");
    }

    private String safeReason(String reason) { return reason == null ? "" : reason.trim(); }
}
