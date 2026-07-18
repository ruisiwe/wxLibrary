package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.domain.WlPointRule;
import java.sql.Date;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 积分规则、余额和任务记录数据访问。 */
public interface WlPointMapper
{
    WlPointRule selectEnabledRule(@Param("eventType") String eventType);
    WlPointRule selectRuleById(@Param("id") Long id);
    List<WlPointRule> selectRuleList(WlPointRule query);
    int updateRule(WlPointRule rule);
    int deductIfEnough(@Param("userId") Long userId, @Param("amount") Long amount);
    int addPoints(@Param("userId") Long userId, @Param("amount") Long amount);
    Long selectBalance(@Param("userId") Long userId);
    int countAdRewards(@Param("userId") Long userId, @Param("rewardDate") Date rewardDate);
    int insertAdReward(@Param("userId") Long userId, @Param("adBizNo") String adBizNo,
            @Param("rewardDate") Date rewardDate, @Param("awardedPoints") Long awardedPoints,
            @Param("pointRecordId") Long pointRecordId);
    int insertSignin(@Param("userId") Long userId, @Param("signinDate") Date signinDate,
            @Param("awardedPoints") Long awardedPoints, @Param("pointRecordId") Long pointRecordId);
    int insertShare(@Param("userId") Long userId, @Param("shareDate") Date shareDate,
            @Param("awardedPoints") Long awardedPoints, @Param("pointRecordId") Long pointRecordId);
    int claimInvitation(@Param("inviterUserId") Long inviterUserId,
            @Param("invitedUserId") Long invitedUserId, @Param("inviteCode") String inviteCode);
    int completeInvitation(@Param("invitedUserId") Long invitedUserId,
            @Param("pointRecordId") Long pointRecordId);
    List<WlPointRecord> selectPointRecords(@Param("userId") Long userId,
            @Param("offset") long offset, @Param("limit") int limit);
    long countPointRecords(@Param("userId") Long userId);
    List<WlPointRecord> selectPointRecordList(WlPointRecord query);
}
