package com.ruoyi.library.auth;

import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.mapper.WlWxUserMapper;
import org.springframework.stereotype.Service;

/** 小程序受保护接口的微信用户状态校验服务。 */
@Service
public class WxUserAccessService
{
    private final WlWxUserMapper userMapper;

    public WxUserAccessService(WlWxUserMapper userMapper)
    {
        this.userMapper = userMapper;
    }

    /** 判断微信用户是否存在且处于正常状态。 */
    public boolean isEnabled(Long userId)
    {
        WlWxUser user = userMapper.selectById(userId);
        return user != null && "0".equals(user.getStatus());
    }
}
