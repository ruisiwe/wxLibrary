package com.ruoyi.library.auth;

import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.mapper.WlWxUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WxUserAccessServiceTest
{
    private WlWxUserMapper userMapper;
    private WxUserAccessService service;

    @BeforeEach
    void setUp()
    {
        userMapper = mock(WlWxUserMapper.class);
        service = new WxUserAccessService(userMapper);
    }

    @Test
    void enabledUserMayAccessProtectedWechatApi()
    {
        when(userMapper.selectById(7L)).thenReturn(user("0"));

        assertTrue(service.isEnabled(7L));
    }

    @Test
    void disabledOrMissingUserCannotAccessProtectedWechatApi()
    {
        when(userMapper.selectById(8L)).thenReturn(user("1"));
        when(userMapper.selectById(9L)).thenReturn(null);

        assertFalse(service.isEnabled(8L));
        assertFalse(service.isEnabled(9L));
    }

    private WlWxUser user(String status)
    {
        WlWxUser user = new WlWxUser();
        user.setStatus(status);
        return user;
    }
}
