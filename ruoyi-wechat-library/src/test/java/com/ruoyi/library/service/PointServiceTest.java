package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.domain.WlPointRule;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.mapper.WlPointMapper;
import com.ruoyi.library.mapper.WlPointRecordMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PointServiceTest
{
    private WlPointMapper pointMapper;
    private WlPointRecordMapper recordMapper;
    private WlWxUserMapper userMapper;
    private PointService service;

    @BeforeEach
    void setUp()
    {
        pointMapper = mock(WlPointMapper.class);
        recordMapper = mock(WlPointRecordMapper.class);
        userMapper = mock(WlWxUserMapper.class);
        when(recordMapper.insertPointRecord(any(WlPointRecord.class))).thenReturn(1);
        when(pointMapper.insertSignin(any(), any(), any(), any())).thenReturn(1);
        when(pointMapper.insertAdReward(any(), any(), any(), any(), any())).thenReturn(1);
        when(pointMapper.insertShare(any(), any(), any(), any())).thenReturn(1);
        Clock clock = Clock.fixed(Instant.parse("2026-07-18T04:00:00Z"), ZoneOffset.UTC);
        service = new PointService(pointMapper, recordMapper, userMapper, clock);
    }

    @Test
    void pointRecoveryStopsAtZeroAndRecordsActualAmount()
    {
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(user(11L, 3L));
        when(pointMapper.deductIfEnough(11L, 3L)).thenReturn(1);

        long recovered = service.deductToFloorZero(11L, 50L, "退款追回", "refund-1");

        assertEquals(3L, recovered);
        ArgumentCaptor<WlPointRecord> captor = ArgumentCaptor.forClass(WlPointRecord.class);
        verify(recordMapper).insertPointRecord(captor.capture());
        assertEquals(-3L, captor.getValue().getChangePoints());
        assertEquals(3L, captor.getValue().getBeforeBalance());
        assertEquals(0L, captor.getValue().getAfterBalance());
    }

    @Test
    void floorDeductionIsIdempotentByBusinessNumber()
    {
        WlPointRecord existing = new WlPointRecord();
        existing.setUserId(11L);
        existing.setEventType("REFUND_RECOVERY");
        existing.setBizNo("refund-1");
        existing.setChangePoints(-3L);
        when(recordMapper.selectByBizNo("refund-1")).thenReturn(existing);

        assertEquals(3L, service.deductToFloorZero(11L, 50L, "退款追回", "refund-1"));
        verify(userMapper, never()).selectByIdForUpdate(any());
    }

    @Test
    void rewardedAdUsesRuleSnapshotAndRejectsSixthReward()
    {
        WlPointRule rule = rule("AD_REWARD", 1L, 5);
        when(pointMapper.selectEnabledRule("AD_REWARD")).thenReturn(rule);
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(user(11L, 8L));
        when(pointMapper.countAdRewards(11L, java.sql.Date.valueOf("2026-07-18"))).thenReturn(4);
        when(pointMapper.addPoints(11L, 1L)).thenReturn(1);

        WlPointRecord record = service.rewardAd(11L, "ad-complete-5");

        assertEquals(1L, record.getChangePoints());
        assertEquals(8L, record.getBeforeBalance());
        assertEquals(9L, record.getAfterBalance());
        assertEquals(rule.getId(), record.getRuleId());
        verify(pointMapper).insertAdReward(11L, "ad-complete-5",
                java.sql.Date.valueOf("2026-07-18"), 1L, record.getId());

        when(pointMapper.countAdRewards(11L, java.sql.Date.valueOf("2026-07-18"))).thenReturn(5);
        assertEquals("今日广告奖励次数已达上限", assertThrows(ServiceException.class,
                () -> service.rewardAd(11L, "ad-complete-6")).getMessage());
    }

    @Test
    void rewardedAdHardLimitCannotBeRaisedAboveFive()
    {
        WlPointRule rule = rule("AD_REWARD", 1L, 99);
        when(pointMapper.selectEnabledRule("AD_REWARD")).thenReturn(rule);
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(user(11L, 8L));
        when(pointMapper.countAdRewards(11L, java.sql.Date.valueOf("2026-07-18"))).thenReturn(5);

        assertEquals("今日广告奖励次数已达上限", assertThrows(ServiceException.class,
                () -> service.rewardAd(11L, "ad-complete-6")).getMessage());
        verify(pointMapper, never()).addPoints(any(), any());
    }

    @Test
    void administratorCannotConfigureAdLimitAboveFive()
    {
        WlPointRule existing = rule("AD_REWARD", 1L, 5);
        WlPointRule update = rule("AD_REWARD", 1L, 6);
        when(pointMapper.selectRuleById(existing.getId())).thenReturn(existing);

        assertEquals("激励视频每日上限不能超过5次", assertThrows(ServiceException.class,
                () -> service.updateRule(update, "admin")).getMessage());
        verify(pointMapper, never()).updateRule(any());
    }

    @Test
    void disabledRuleDoesNotGrantPoints()
    {
        when(pointMapper.selectEnabledRule("SIGN_IN")).thenReturn(null);
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(user(11L, 8L));

        assertEquals("当前积分任务未启用", assertThrows(ServiceException.class,
                () -> service.signIn(11L)).getMessage());
        verify(pointMapper, never()).addPoints(any(), any());
    }

    @Test
    void invitationRewardIsIdempotentByInvitedUser()
    {
        WlPointRecord existing = new WlPointRecord();
        existing.setId(91L);
        existing.setUserId(11L);
        existing.setEventType("INVITE");
        existing.setBizNo("INVITE:12");
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(user(11L, 8L));
        when(recordMapper.selectByBizNo("INVITE:12")).thenReturn(existing);

        assertEquals(existing, service.rewardInvitation(11L, 12L, "invite-12"));
        verify(pointMapper, never()).claimInvitation(any(), any(), any());
        verify(pointMapper, never()).addPoints(any(), any());
    }

    @Test
    void disabledUserCannotReceiveIdempotentTaskResult()
    {
        WlWxUser disabled = user(11L, 8L);
        disabled.setStatus("1");
        when(userMapper.selectByIdForUpdate(11L)).thenReturn(disabled);
        WlPointRecord existing = new WlPointRecord();
        existing.setUserId(11L);
        existing.setEventType("SIGN_IN");
        existing.setBizNo("SIGN_IN:11:2026-07-18");
        when(recordMapper.selectByBizNo(existing.getBizNo())).thenReturn(existing);

        assertEquals("当前账号已停用，请联系管理员", assertThrows(ServiceException.class,
                () -> service.signIn(11L)).getMessage());
        verify(recordMapper, never()).selectByBizNo(existing.getBizNo());
    }

    private WlWxUser user(Long id, Long balance)
    {
        WlWxUser user = new WlWxUser();
        user.setId(id);
        user.setPointBalance(balance);
        user.setStatus("0");
        return user;
    }

    private WlPointRule rule(String eventType, Long value, Integer dailyLimit)
    {
        WlPointRule rule = new WlPointRule();
        rule.setId(3L);
        rule.setEventType(eventType);
        rule.setRuleName("激励视频广告");
        rule.setPointValue(value);
        rule.setDailyLimit(dailyLimit);
        rule.setStatus("0");
        return rule;
    }
}
