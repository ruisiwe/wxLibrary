package com.ruoyi.library.auth;

import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.agreement.WxAgreementService;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.WxLoginRequest;
import com.ruoyi.library.dto.WxLoginResponse;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.storage.AvatarStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.inOrder;
import org.mockito.InOrder;

class WxLoginServiceTest
{
    private WechatCodeClient codeClient;
    private WlWxUserMapper userMapper;
    private WxAgreementService agreementService;
    private AvatarStorageService avatarStorageService;
    private WxTokenService tokenService;
    private WxLoginService loginService;

    @BeforeEach
    void setUp()
    {
        codeClient = mock(WechatCodeClient.class);
        userMapper = mock(WlWxUserMapper.class);
        agreementService = mock(WxAgreementService.class);
        avatarStorageService = mock(AvatarStorageService.class);
        tokenService = mock(WxTokenService.class);
        loginService = new WxLoginService(codeClient, userMapper, agreementService,
                avatarStorageService, tokenService);
        when(codeClient.exchange("code")).thenReturn("openid-secret");
    }

    @Test
    void firstLoginCreatesIndependentWechatUserAcceptsAgreementsAndIssuesToken()
    {
        WxLoginRequest request = completeRequest();
        MockMultipartFile avatar = avatar();
        when(avatarStorageService.store(avatar)).thenReturn("202607/avatar.jpg");
        doAnswer(invocation -> {
            WlWxUser user = invocation.getArgument(0);
            user.setId(18L);
            return 1;
        }).when(userMapper).insertWxUser(any(WlWxUser.class));
        when(agreementService.hasAcceptedAllCurrent(18L)).thenReturn(true);
        when(tokenService.issue(18L)).thenReturn("wx-token");

        WxLoginResponse response = loginService.login(request, avatar, "127.0.0.1");

        ArgumentCaptor<WlWxUser> captor = ArgumentCaptor.forClass(WlWxUser.class);
        verify(userMapper).insertWxUser(captor.capture());
        assertEquals("openid-secret", captor.getValue().getOpenid());
        assertEquals("测试用户", captor.getValue().getNickname());
        assertEquals("202607/avatar.jpg", captor.getValue().getAvatarPath());
        verify(agreementService).validateCurrentAcceptance(true, "privacy-v1", true, "statement-v1");
        verify(agreementService).acceptCurrent(18L, "privacy-v1", "statement-v1", "127.0.0.1");
        assertEquals("wx-token", response.getToken());
        assertFalse(response.isAgreementRequired());
        assertFalse(JSON.toJSONString(response).contains("openid"));
        assertThrows(NoSuchMethodException.class, () -> WxLoginResponse.class.getMethod("getOpenid"));
    }

    @Test
    void missingOpenidDoesNotAcquireGapLockBeforeInsert()
    {
        WxLoginRequest request = completeRequest();
        MockMultipartFile avatar = avatar();
        when(userMapper.selectByOpenid("openid-secret")).thenReturn(null);
        when(avatarStorageService.store(avatar)).thenReturn("202607/avatar.jpg");
        doAnswer(invocation -> {
            WlWxUser user = invocation.getArgument(0);
            user.setId(18L);
            return 1;
        }).when(userMapper).insertWxUser(any(WlWxUser.class));
        when(agreementService.hasAcceptedAllCurrent(18L)).thenReturn(true);
        when(tokenService.issue(18L)).thenReturn("wx-token");

        loginService.login(request, avatar, "127.0.0.1");

        InOrder order = inOrder(userMapper);
        order.verify(userMapper).selectByOpenid("openid-secret");
        order.verify(userMapper).insertWxUser(any(WlWxUser.class));
        verify(userMapper, never()).selectByOpenidForUpdate("openid-secret");
    }

    @Test
    void firstLoginRequiresAvatar()
    {
        ServiceException exception = assertThrows(ServiceException.class,
                () -> loginService.login(completeRequest(), null, null));
        assertEquals("首次登录必须上传有效头像", exception.getMessage());
        verify(userMapper, never()).insertWxUser(any(WlWxUser.class));
    }

    @Test
    void firstLoginRequiresCompliantNickname()
    {
        WxLoginRequest request = completeRequest();
        request.setNickname("<script>alert(1)</script>");

        ServiceException exception = assertThrows(ServiceException.class,
                () -> loginService.login(request, avatar(), null));
        assertEquals("昵称不能包含HTML标签或控制字符", exception.getMessage());
    }

    @Test
    void firstLoginRequiresNicknameAndBothCurrentAgreements()
    {
        WxLoginRequest missingNickname = completeRequest();
        missingNickname.setNickname(" ");
        assertEquals("首次登录必须填写昵称", assertThrows(ServiceException.class,
                () -> loginService.login(missingNickname, avatar(), null)).getMessage());

        WxLoginRequest missingPrivacy = completeRequest();
        missingPrivacy.setPrivacyAccepted(false);
        doThrow(new ServiceException("请勾选用户隐私协议")).when(agreementService)
                .validateCurrentAcceptance(false, "privacy-v1", true, "statement-v1");
        assertEquals("请勾选用户隐私协议", assertThrows(ServiceException.class,
                () -> loginService.login(missingPrivacy, avatar(), null)).getMessage());
    }

    @Test
    void firstLoginRejectsUnicodeControlAndFormatCharacters()
    {
        WxLoginRequest control = completeRequest();
        control.setNickname("用户" + new String(Character.toChars(0x85)));
        assertEquals("昵称不能包含HTML标签或控制字符", assertThrows(ServiceException.class,
                () -> loginService.login(control, avatar(), null)).getMessage());

        WxLoginRequest format = completeRequest();
        format.setNickname("用户" + new String(Character.toChars(0x202E)));
        assertEquals("昵称不能包含HTML标签或控制字符", assertThrows(ServiceException.class,
                () -> loginService.login(format, avatar(), null)).getMessage());
    }

    @Test
    void existingUserCanLoginWithoutRepeatedProfileInput()
    {
        WlWxUser existing = user(9L, "旧昵称", "202601/old.png", "0");
        when(userMapper.selectByOpenid("openid-secret")).thenReturn(existing);
        when(userMapper.selectByOpenidForUpdate("openid-secret")).thenReturn(existing);
        when(agreementService.hasAcceptedAllCurrent(9L)).thenReturn(true);
        when(tokenService.issue(9L)).thenReturn("token");
        WxLoginRequest request = new WxLoginRequest();
        request.setCode("code");

        WxLoginResponse response = loginService.login(request, null, null);

        assertEquals("旧昵称", response.getNickname());
        assertEquals("202601/old.png", response.getAvatarPath());
        verify(avatarStorageService, never()).store(any());
        verify(userMapper, never()).updateProfile(any(WlWxUser.class));
        verify(userMapper).updateLastLoginTime(9L);
    }

    @Test
    void disabledUserCannotLogin()
    {
        when(userMapper.selectByOpenid("openid-secret"))
                .thenReturn(user(9L, "用户", "202601/a.png", "1"));
        when(userMapper.selectByOpenidForUpdate("openid-secret"))
                .thenReturn(user(9L, "用户", "202601/a.png", "1"));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> loginService.login(codeOnly(), null, null));
        assertEquals("该微信用户已被停用，无法登录", exception.getMessage());
        verify(tokenService, never()).issue(anyLong());
    }

    @Test
    void existingUserMayLoginWhenNewAgreementIsNotAccepted()
    {
        when(userMapper.selectByOpenid("openid-secret"))
                .thenReturn(user(9L, "用户", "202601/a.png", "0"));
        when(userMapper.selectByOpenidForUpdate("openid-secret"))
                .thenReturn(user(9L, "用户", "202601/a.png", "0"));
        when(agreementService.hasAcceptedAllCurrent(9L)).thenReturn(false);
        when(tokenService.issue(9L)).thenReturn("token");

        WxLoginResponse response = loginService.login(codeOnly(), null, null);

        assertTrue(response.isAgreementRequired());
        verify(agreementService, never()).acceptCurrent(anyLong(), any(), any(), any());
    }

    @Test
    void duplicateOpenidDuringFirstLoginRecoversExistingUser()
    {
        WlWxUser concurrent = user(21L, "并发用户", "202607/a.jpg", "0");
        when(userMapper.selectByOpenid("openid-secret")).thenReturn(null);
        when(userMapper.selectByOpenidForUpdate("openid-secret"))
                .thenReturn(concurrent);
        when(userMapper.insertWxUser(any(WlWxUser.class)))
                .thenThrow(new DuplicateKeyException("uk_wx_user_openid"));
        when(agreementService.hasAcceptedAllCurrent(21L)).thenReturn(true);
        when(tokenService.issue(21L)).thenReturn("token");

        WxLoginResponse response = loginService.login(completeRequest(), avatar(), "127.0.0.1");

        assertEquals(21L, response.getUserId());
        verify(userMapper).updateLastLoginTime(21L);
    }

    private WxLoginRequest completeRequest()
    {
        WxLoginRequest request = codeOnly();
        request.setNickname("测试用户");
        request.setPrivacyAccepted(true);
        request.setPrivacyVersion("privacy-v1");
        request.setStatementAccepted(true);
        request.setStatementVersion("statement-v1");
        return request;
    }

    private WxLoginRequest codeOnly()
    {
        WxLoginRequest request = new WxLoginRequest();
        request.setCode("code");
        return request;
    }

    private MockMultipartFile avatar()
    {
        return new MockMultipartFile("avatar", "avatar.jpg", "image/jpeg", new byte[] {1});
    }

    private WlWxUser user(Long id, String nickname, String avatarPath, String status)
    {
        WlWxUser user = new WlWxUser();
        user.setId(id);
        user.setOpenid("openid-secret");
        user.setNickname(nickname);
        user.setAvatarPath(avatarPath);
        user.setPointBalance(0L);
        user.setStatus(status);
        return user;
    }
}
