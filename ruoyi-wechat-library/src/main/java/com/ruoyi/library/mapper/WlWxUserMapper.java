package com.ruoyi.library.mapper;

import java.util.List;
import java.util.Date;
import com.ruoyi.library.domain.WlWxUser;
import org.apache.ibatis.annotations.Param;

/** 微信用户数据访问。 */
public interface WlWxUserMapper
{
    WlWxUser selectByOpenid(@Param("openid") String openid);
    WlWxUser selectByOpenidForUpdate(@Param("openid") String openid);
    WlWxUser selectById(@Param("id") Long id);
    WlWxUser selectByIdForUpdate(@Param("id") Long id);
    List<WlWxUser> selectWxUserList(WlWxUser user);
    List<WlWxUser> selectVipOperationCandidates(@Param("keyword") String keyword,
            @Param("userId") Long userId);
    int insertWxUser(WlWxUser user);
    int updateProfile(WlWxUser user);
    int updateLastLoginTime(@Param("id") Long id);
    int updateStatus(@Param("id") Long id, @Param("status") String status, @Param("operator") String operator);
    int updatePointBalance(@Param("id") Long id, @Param("beforeBalance") Long beforeBalance,
            @Param("afterBalance") Long afterBalance, @Param("operator") String operator);
    int updateVipExpireTime(@Param("id") Long id, @Param("vipExpireTime") Date vipExpireTime,
            @Param("operator") String operator);
}
