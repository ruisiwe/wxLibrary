package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.auth.WxLoginService;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.WxProfileResponse;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.storage.AvatarStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WxProfileServiceTest
{
    private WlWxUserMapper mapper;
    private AvatarStorageService storage;
    private WxLoginService loginService;
    private WxProfileService service;

    @BeforeEach
    void setUp()
    {
        mapper = mock(WlWxUserMapper.class);
        storage = mock(AvatarStorageService.class);
        loginService = mock(WxLoginService.class);
        service = new WxProfileService(mapper, storage, loginService);
    }

    @Test
    void readsProfileWithoutExposingOpenid()
    {
        WlWxUser user = user();
        when(mapper.selectById(3L)).thenReturn(user);
        WxProfileResponse response = service.get(3L);
        assertEquals("昵称", response.getNickname());
        assertThrows(NoSuchMethodException.class, () -> WxProfileResponse.class.getMethod("getOpenid"));
    }

    @Test
    void profileIncludesCurrentVipState()
    {
        WlWxUser user = user();
        user.setVipExpireTime(new Date(4102444800000L));
        when(mapper.selectById(3L)).thenReturn(user);

        WxProfileResponse response = service.get(3L);

        assertEquals(user.getVipExpireTime(), response.getVipExpireTime());
        assertEquals(true, response.isVipActive());
    }

    @Test
    void updatesOnlyNicknameAndAvatar()
    {
        WlWxUser user = user();
        when(mapper.selectByIdForUpdate(3L)).thenReturn(user);
        when(loginService.validateNickname("新昵称", true)).thenReturn("新昵称");
        MockMultipartFile avatar = new MockMultipartFile("avatar", "a.png", "image/png", new byte[] {1});
        when(storage.store(avatar)).thenReturn("202607/new.png");
        when(mapper.updateProfile(user)).thenReturn(1);

        service.update(3L, "新昵称", avatar);

        assertEquals("新昵称", user.getNickname());
        assertEquals("202607/new.png", user.getAvatarPath());
        verify(mapper).updateProfile(user);
    }

    @Test
    void rejectsDisabledOrMissingUser()
    {
        assertEquals("微信用户不存在", assertThrows(ServiceException.class,
                () -> service.get(99L)).getMessage());
        WlWxUser disabled = user();
        disabled.setStatus("1");
        when(mapper.selectByIdForUpdate(3L)).thenReturn(disabled);
        assertEquals("该微信用户已被停用，无法修改资料", assertThrows(ServiceException.class,
                () -> service.update(3L, "昵称", null)).getMessage());
    }

    @Test
    void profileUpdateRejectsUnicodeFormatCharacter()
    {
        WlWxUser user = user();
        when(mapper.selectByIdForUpdate(3L)).thenReturn(user);
        WxLoginService realValidator = new WxLoginService(mock(com.ruoyi.library.auth.WechatCodeClient.class),
                mapper, mock(com.ruoyi.library.agreement.WxAgreementService.class), storage,
                mock(com.ruoyi.library.auth.WxTokenService.class));
        WxProfileService profileService = new WxProfileService(mapper, storage, realValidator);

        assertEquals("昵称不能包含HTML标签或控制字符", assertThrows(ServiceException.class,
                () -> profileService.update(3L, "用户" + new String(Character.toChars(0x202E)), null)).getMessage());
    }

    private WlWxUser user()
    {
        WlWxUser user = new WlWxUser();
        user.setId(3L);
        user.setOpenid("secret-openid");
        user.setNickname("昵称");
        user.setAvatarPath("202607/a.png");
        user.setPointBalance(5L);
        user.setStatus("0");
        return user;
    }
}
