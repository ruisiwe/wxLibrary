package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.domain.WlPointRule;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.PageResult;
import com.ruoyi.library.mapper.WlPointMapper;
import com.ruoyi.library.mapper.WlPointRecordMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import java.sql.Date;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 积分规则、任务奖励、扣减和流水服务。 */
@Service
public class PointService
{
    private static final Set<String> RULE_EVENTS = new HashSet<>(
            Arrays.asList("SIGN_IN", "AD_REWARD", "SHARE", "INVITE"));
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Shanghai");

    private final WlPointMapper pointMapper;
    private final WlPointRecordMapper recordMapper;
    private final WlWxUserMapper userMapper;
    private final Clock clock;

    public PointService(WlPointMapper pointMapper, WlPointRecordMapper recordMapper,
            WlWxUserMapper userMapper)
    {
        this(pointMapper, recordMapper, userMapper, Clock.systemUTC());
    }

    PointService(WlPointMapper pointMapper, WlPointRecordMapper recordMapper,
            WlWxUserMapper userMapper, Clock clock)
    {
        this.pointMapper = pointMapper;
        this.recordMapper = recordMapper;
        this.userMapper = userMapper;
        this.clock = clock;
    }

    public Long getBalance(Long userId)
    {
        WlWxUser user = requireEnabledUser(userMapper.selectById(userId));
        return user.getPointBalance() == null ? 0L : user.getPointBalance();
    }

    public PageResult<WlPointRecord> listRecords(Long userId, int pageNum, int pageSize)
    {
        requireEnabledUser(userMapper.selectById(userId));
        int safePageNum = Math.max(pageNum, 1);
        int safePageSize = pageSize < 1 ? 20 : Math.min(pageSize, 50);
        long total = pointMapper.countPointRecords(userId);
        List<WlPointRecord> items = total == 0L ? java.util.Collections.<WlPointRecord>emptyList()
                : pointMapper.selectPointRecords(userId, ((long) safePageNum - 1L) * safePageSize, safePageSize);
        return new PageResult<>(items, total, safePageNum, safePageSize);
    }

    @Transactional
    public WlPointRecord deduct(Long userId, Long amount, String eventType, String bizNo, String description)
    {
        WlWxUser locked = requireEnabledUser(userMapper.selectByIdForUpdate(userId));
        return deductAfterLock(locked, amount, eventType, bizNo, description);
    }

    /** 仅由已持有微信用户行锁的同事务服务调用。 */
    WlPointRecord deductAfterLock(WlWxUser lockedUser, Long amount, String eventType,
            String bizNo, String description)
    {
        validateAmount(amount);
        validateEventAndBiz(eventType, bizNo);
        requireEnabledUser(lockedUser);
        WlPointRecord existing = findIdempotent(lockedUser.getId(), eventType, bizNo);
        if (existing != null) return existing;
        long before = balanceOf(lockedUser);
        if (before < amount) throw new ServiceException("积分不足，请先获取积分");
        if (amount > 0 && pointMapper.deductIfEnough(lockedUser.getId(), amount) != 1)
            throw new ServiceException("积分余额已变化，请重试");
        return insertRecord(lockedUser.getId(), null, eventType, bizNo, -amount,
                before, before - amount, description);
    }

    @Transactional
    public long deductToFloorZero(Long userId, Long requestedAmount, String description, String bizNo)
    {
        validateAmount(requestedAmount);
        WlPointRecord existing = findIdempotent(userId, "REFUND_RECOVERY", bizNo);
        if (existing != null) return Math.abs(existing.getChangePoints());
        WlWxUser locked = requireEnabledUser(userMapper.selectByIdForUpdate(userId));
        existing = findIdempotent(userId, "REFUND_RECOVERY", bizNo);
        if (existing != null) return Math.abs(existing.getChangePoints());
        long before = balanceOf(locked);
        long actual = Math.min(before, requestedAmount);
        if (actual > 0 && pointMapper.deductIfEnough(userId, actual) != 1)
            throw new ServiceException("积分余额已变化，请重试");
        insertRecord(userId, null, "REFUND_RECOVERY", bizNo, -actual,
                before, before - actual, description);
        return actual;
    }

    @Transactional
    public WlPointRecord signIn(Long userId)
    {
        LocalDate today = today();
        String bizNo = "SIGN_IN:" + userId + ":" + today;
        WlWxUser locked = requireEnabledUser(userMapper.selectByIdForUpdate(userId));
        WlPointRecord existing = findIdempotent(userId, "SIGN_IN", bizNo);
        if (existing != null) return existing;
        WlPointRule rule = requireRule("SIGN_IN");
        WlPointRecord record = creditAfterLock(locked, rule, bizNo, "每日签到奖励");
        if (pointMapper.insertSignin(userId, Date.valueOf(today), record.getChangePoints(), record.getId()) != 1)
            throw new ServiceException("签到奖励记录失败，请重试");
        return record;
    }

    @Transactional
    public WlPointRecord rewardAd(Long userId, String adBizNo)
    {
        requireText(adBizNo, "广告完成业务编号不能为空");
        String bizNo = "AD_REWARD:" + userId + ":" + adBizNo.trim();
        WlWxUser locked = requireEnabledUser(userMapper.selectByIdForUpdate(userId));
        WlPointRecord existing = findIdempotent(userId, "AD_REWARD", bizNo);
        if (existing != null) return existing;
        WlPointRule rule = requireRule("AD_REWARD");
        LocalDate today = today();
        int limit = rule.getDailyLimit() == null ? 0 : rule.getDailyLimit();
        if (limit <= 0 || pointMapper.countAdRewards(userId, Date.valueOf(today)) >= limit)
            throw new ServiceException("今日广告奖励次数已达上限");
        WlPointRecord record = creditAfterLock(locked, rule, bizNo, "完整观看激励视频广告");
        if (pointMapper.insertAdReward(userId, adBizNo.trim(), Date.valueOf(today),
                record.getChangePoints(), record.getId()) != 1)
            throw new ServiceException("广告奖励记录失败，请重试");
        return record;
    }

    @Transactional
    public WlPointRecord rewardShare(Long userId)
    {
        LocalDate today = today();
        String bizNo = "SHARE:" + userId + ":" + today;
        WlWxUser locked = requireEnabledUser(userMapper.selectByIdForUpdate(userId));
        WlPointRecord existing = findIdempotent(userId, "SHARE", bizNo);
        if (existing != null) return existing;
        WlPointRule rule = requireRule("SHARE");
        WlPointRecord record = creditAfterLock(locked, rule, bizNo, "每日分享奖励");
        if (pointMapper.insertShare(userId, Date.valueOf(today), record.getChangePoints(), record.getId()) != 1)
            throw new ServiceException("分享奖励记录失败，请重试");
        return record;
    }

    @Transactional
    public WlPointRecord rewardInvitation(Long inviterUserId, Long invitedUserId, String inviteCode)
    {
        if (inviterUserId == null || invitedUserId == null || inviterUserId <= 0 || invitedUserId <= 0)
            throw new ServiceException("邀请用户编号不正确");
        if (inviterUserId.equals(invitedUserId)) throw new ServiceException("不能邀请自己");
        requireText(inviteCode, "邀请标识不能为空");
        if (inviteCode.trim().length() > 64) throw new ServiceException("邀请标识不能超过64个字符");
        WlWxUser locked = requireEnabledUser(userMapper.selectByIdForUpdate(inviterUserId));
        String bizNo = "INVITE:" + invitedUserId;
        WlPointRecord existing = findIdempotent(inviterUserId, "INVITE", bizNo);
        if (existing != null) return existing;
        if (pointMapper.claimInvitation(inviterUserId, invitedUserId, inviteCode.trim()) != 1)
            throw new ServiceException("该用户已记录邀请关系");
        WlPointRecord record = creditAfterLock(locked, requireRule("INVITE"), bizNo, "邀请新用户奖励");
        completeInvitation(invitedUserId, record.getId());
        return record;
    }

    public List<WlPointRule> listRules(WlPointRule query)
    {
        return pointMapper.selectRuleList(query == null ? new WlPointRule() : query);
    }

    public List<WlPointRecord> listAdminRecords(WlPointRecord query)
    {
        return pointMapper.selectPointRecordList(query == null ? new WlPointRecord() : query);
    }

    public WlPointRule getRule(Long id)
    {
        WlPointRule rule = pointMapper.selectRuleById(id);
        if (rule == null) throw new ServiceException("积分规则不存在");
        return rule;
    }

    public int updateRule(WlPointRule rule, String operator)
    {
        if (rule == null || rule.getId() == null) throw new ServiceException("积分规则编号不能为空");
        WlPointRule existing = getRule(rule.getId());
        if (!RULE_EVENTS.contains(existing.getEventType())) throw new ServiceException("积分事件类型不正确");
        if (rule.getPointValue() == null || rule.getPointValue() < 0)
            throw new ServiceException("奖励积分不能小于0");
        if (rule.getDailyLimit() != null && rule.getDailyLimit() < 1)
            throw new ServiceException("每日上限必须大于0");
        if (!"0".equals(rule.getStatus()) && !"1".equals(rule.getStatus()))
            throw new ServiceException("积分规则状态不正确");
        if (rule.getRemark() != null && rule.getRemark().length() > 500)
            throw new ServiceException("积分规则备注不能超过500个字符");
        rule.setEventType(existing.getEventType());
        rule.setRuleName(existing.getRuleName());
        rule.setUpdateBy(operator);
        return pointMapper.updateRule(rule);
    }

    private WlPointRecord creditAfterLock(WlWxUser locked, WlPointRule rule, String bizNo, String description)
    {
        long before = balanceOf(locked);
        long after;
        try { after = Math.addExact(before, rule.getPointValue()); }
        catch (ArithmeticException exception) { throw new ServiceException("积分余额超出范围"); }
        if (rule.getPointValue() > 0 && pointMapper.addPoints(locked.getId(), rule.getPointValue()) != 1)
            throw new ServiceException("积分余额已变化，请重试");
        return insertRecord(locked.getId(), rule.getId(), rule.getEventType(), bizNo,
                rule.getPointValue(), before, after, description);
    }

    /** 仅由已持有微信用户行锁的会员事务调用，按业务编号幂等赠送固定积分。 */
    WlPointRecord creditFixedAfterLock(WlWxUser locked, Long amount, String bizNo, String description)
    {
        validateAmount(amount);
        requireEnabledUser(locked);
        WlPointRecord existing = findIdempotent(locked.getId(), "VIP_GIFT", bizNo);
        if (existing != null) return existing;
        long before = balanceOf(locked);
        long after;
        try { after = Math.addExact(before, amount); }
        catch (ArithmeticException exception) { throw new ServiceException("积分余额超出范围"); }
        if (amount > 0 && pointMapper.addPoints(locked.getId(), amount) != 1)
            throw new ServiceException("积分余额已变化，请重试");
        WlPointRecord record = insertRecord(locked.getId(), null, "VIP_GIFT", bizNo,
                amount, before, after, description);
        locked.setPointBalance(after);
        return record;
    }

    private void completeInvitation(Long invitedUserId, Long pointRecordId)
    {
        if (pointMapper.completeInvitation(invitedUserId, pointRecordId) != 1)
            throw new ServiceException("邀请奖励状态更新失败，请重试");
    }

    private WlPointRecord insertRecord(Long userId, Long ruleId, String eventType, String bizNo,
            Long change, Long before, Long after, String description)
    {
        WlPointRecord record = new WlPointRecord();
        record.setUserId(userId);
        record.setRuleId(ruleId);
        record.setEventType(eventType);
        record.setBizNo(bizNo);
        record.setChangePoints(change);
        record.setBeforeBalance(before);
        record.setAfterBalance(after);
        String safeDescription = description == null ? "" : description.trim();
        record.setDescription(safeDescription.length() <= 255
                ? safeDescription : safeDescription.substring(0, 255));
        record.setCreateBy("system");
        if (recordMapper.insertPointRecord(record) != 1)
            throw new ServiceException("积分流水记录失败，请重试");
        return record;
    }

    private WlPointRecord findIdempotent(Long userId, String eventType, String bizNo)
    {
        validateEventAndBiz(eventType, bizNo);
        WlPointRecord existing = recordMapper.selectByBizNo(bizNo);
        if (existing == null) return null;
        if (!userId.equals(existing.getUserId()) || !eventType.equals(existing.getEventType()))
            throw new ServiceException("积分业务编号已被其他操作使用");
        return existing;
    }

    private WlPointRule requireRule(String eventType)
    {
        WlPointRule rule = pointMapper.selectEnabledRule(eventType);
        if (rule == null) throw new ServiceException("当前积分任务未启用");
        if (rule.getPointValue() == null || rule.getPointValue() < 0)
            throw new ServiceException("积分规则配置不正确");
        return rule;
    }

    private WlWxUser requireEnabledUser(WlWxUser user)
    {
        if (user == null) throw new ServiceException("微信用户不存在");
        if (!"0".equals(user.getStatus())) throw new ServiceException("当前账号已停用，请联系管理员");
        return user;
    }

    private long balanceOf(WlWxUser user) { return user.getPointBalance() == null ? 0L : user.getPointBalance(); }

    private void validateAmount(Long amount)
    {
        if (amount == null || amount < 0) throw new ServiceException("积分数量不能小于0");
    }

    private void validateEventAndBiz(String eventType, String bizNo)
    {
        requireText(eventType, "积分事件类型不能为空");
        requireText(bizNo, "积分业务编号不能为空");
        if (eventType.trim().length() > 32) throw new ServiceException("积分事件类型不能超过32个字符");
        if (bizNo.trim().length() > 128) throw new ServiceException("积分业务编号不能超过128个字符");
    }

    private void requireText(String value, String message)
    {
        if (value == null || value.trim().isEmpty()) throw new ServiceException(message);
    }

    private LocalDate today() { return LocalDate.now(clock.withZone(BUSINESS_ZONE)); }
}
