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
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.TransactionSystemException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
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
    void firstLoginCreatesIndependentWechatUserAndIssuesTokenBeforePrivacyAcceptance()
    {
        WxLoginRequest request = completeRequest();
        MockMultipartFile avatar = avatar();
        when(avatarStorageService.store(avatar)).thenReturn("202607/avatar.jpg");
        doAnswer(invocation -> {
            WlWxUser user = invocation.getArgument(0);
            user.setId(18L);
            return 1;
        }).when(userMapper).insertWxUser(any(WlWxUser.class));
        when(agreementService.hasAcceptedAllCurrent(18L)).thenReturn(false);
        when(tokenService.issue(18L)).thenReturn("wx-token");

        WxLoginResponse response = loginService.login(request, avatar, "127.0.0.1");

        ArgumentCaptor<WlWxUser> captor = ArgumentCaptor.forClass(WlWxUser.class);
        verify(userMapper).insertWxUser(captor.capture());
        assertEquals("openid-secret", captor.getValue().getOpenid());
        assertTrue(captor.getValue().getNickname().matches("[A-Za-z]{10}"));
        assertFalse("测试用户".equals(captor.getValue().getNickname()));
        assertEquals("202607/avatar.jpg", captor.getValue().getAvatarPath());
        verify(userMapper).insertDailyActive(18L);
        verify(agreementService, never()).validateCurrentAcceptance(anyBoolean(), any());
        verify(agreementService, never()).acceptCurrent(anyLong(), any(), any());
        assertEquals("wx-token", response.getToken());
        assertTrue(response.isAgreementRequired());
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
    void firstLoginIgnoresSubmittedNickname()
    {
        WxLoginRequest request = completeRequest();
        request.setNickname("<script>alert(1)</script>");
        when(avatarStorageService.store(any())).thenReturn("202607/avatar.jpg");
        doAnswer(invocation -> {
            WlWxUser user = invocation.getArgument(0);
            user.setId(18L);
            return 1;
        }).when(userMapper).insertWxUser(any(WlWxUser.class));
        when(agreementService.hasAcceptedAllCurrent(18L)).thenReturn(false);
        when(tokenService.issue(18L)).thenReturn("wx-token");

        loginService.login(request, avatar(), null);

        ArgumentCaptor<WlWxUser> captor = ArgumentCaptor.forClass(WlWxUser.class);
        verify(userMapper).insertWxUser(captor.capture());
        assertTrue(captor.getValue().getNickname().matches("[A-Za-z]{10}"));
        assertFalse(request.getNickname().equals(captor.getValue().getNickname()));
    }

    @Test
    void firstLoginGeneratesRandomNicknameWhenNicknameIsMissing()
    {
        WxLoginRequest missingNickname = completeRequest();
        missingNickname.setNickname(" ");
        when(avatarStorageService.store(any())).thenReturn("202607/avatar.jpg");
        doAnswer(invocation -> {
            WlWxUser user = invocation.getArgument(0);
            user.setId(18L);
            return 1;
        }).when(userMapper).insertWxUser(any(WlWxUser.class));
        when(agreementService.hasAcceptedAllCurrent(18L)).thenReturn(false);
        when(tokenService.issue(18L)).thenReturn("wx-token");

        loginService.login(missingNickname, avatar(), null);

        ArgumentCaptor<WlWxUser> captor = ArgumentCaptor.forClass(WlWxUser.class);
        verify(userMapper).insertWxUser(captor.capture());
        assertTrue(captor.getValue().getNickname().matches("[A-Za-z]{10}"));
    }

    @Test
    void firstLoginChecksGeneratedNicknameUntilItIsAvailable()
    {
        when(avatarStorageService.store(any())).thenReturn("202607/avatar.jpg");
        when(userMapper.countByNickname(anyString(), isNull())).thenReturn(1, 0);
        doAnswer(invocation -> {
            WlWxUser user = invocation.getArgument(0);
            user.setId(18L);
            return 1;
        }).when(userMapper).insertWxUser(any(WlWxUser.class));
        when(agreementService.hasAcceptedAllCurrent(18L)).thenReturn(false);
        when(tokenService.issue(18L)).thenReturn("wx-token");

        WxLoginResponse response = loginService.login(completeRequest(), avatar(), null);

        assertEquals(18L, response.getUserId());
        verify(userMapper, times(2)).countByNickname(anyString(), isNull());
        verify(userMapper).insertWxUser(any(WlWxUser.class));
    }

    @Test
    void firstLoginRetriesWhenNicknameConflictsDuringInsert()
    {
        when(userMapper.selectByOpenid("openid-secret")).thenReturn(null);
        when(avatarStorageService.store(any())).thenReturn("202607/avatar.jpg");
        when(userMapper.countByNickname(anyString(), isNull())).thenReturn(0);
        when(userMapper.insertWxUser(any(WlWxUser.class)))
                .thenThrow(new DuplicateKeyException("uk_wx_user_nickname"))
                .thenAnswer(invocation -> {
                    WlWxUser user = invocation.getArgument(0);
                    user.setId(18L);
                    return 1;
                });
        when(agreementService.hasAcceptedAllCurrent(18L)).thenReturn(false);
        when(tokenService.issue(18L)).thenReturn("wx-token");

        WxLoginResponse response = loginService.login(completeRequest(), avatar(), null);

        assertEquals(18L, response.getUserId());
        verify(userMapper, times(2)).insertWxUser(any(WlWxUser.class));
        verify(avatarStorageService).store(any());
        verify(avatarStorageService, never()).deleteQuietly("202607/avatar.jpg");
    }

    @Test
    void existingUserNicknameUpdateRejectsUnicodeControlAndFormatCharacters()
    {
        WlWxUser existing = user(9L, "旧昵称", "202601/old.png", "0");
        when(userMapper.selectByOpenid("openid-secret")).thenReturn(existing);
        when(userMapper.selectByOpenidForUpdate("openid-secret")).thenReturn(existing);

        WxLoginRequest control = codeOnly();
        control.setNickname("用户" + new String(Character.toChars(0x85)));
        assertEquals("昵称只能包含中文、英文字母、数字、下划线和短横线", assertThrows(ServiceException.class,
                () -> loginService.login(control, null, null)).getMessage());

        WxLoginRequest format = codeOnly();
        format.setNickname("用户" + new String(Character.toChars(0x202E)));
        assertEquals("昵称只能包含中文、英文字母、数字、下划线和短横线", assertThrows(ServiceException.class,
                () -> loginService.login(format, null, null)).getMessage());
    }

    @Test
    void nicknameValidationAcceptsWhitelistAndTwentyUnicodeCharacters()
    {
        assertEquals("中文Abc_12-", loginService.validateNickname(" 中文Abc_12- ", true));
        assertEquals("一二三四五六七八九十一二三四五六七八九十",
                loginService.validateNickname("一二三四五六七八九十一二三四五六七八九十", true));
    }

    @Test
    void nicknameValidationRejectsNullBlankAndTooLongValues()
    {
        assertEquals("昵称不能为空", assertThrows(ServiceException.class,
                () -> loginService.validateNickname(null, true)).getMessage());
        assertEquals("昵称不能为空", assertThrows(ServiceException.class,
                () -> loginService.validateNickname("   ", true)).getMessage());
        assertEquals("昵称长度不能超过20个字符", assertThrows(ServiceException.class,
                () -> loginService.validateNickname("一二三四五六七八九十一二三四五六七八九十一", true)).getMessage());
    }

    @Test
    void nicknameValidationRejectsReservedNamesIgnoringCase()
    {
        assertEquals("昵称不能使用保留名称", assertThrows(ServiceException.class,
                () -> loginService.validateNickname("NuLl", true)).getMessage());
        assertEquals("昵称不能使用保留名称", assertThrows(ServiceException.class,
                () -> loginService.validateNickname("UNDEFINED", true)).getMessage());
    }

    @Test
    void nicknameValidationRejectsCharactersOutsideWhitelist()
    {
        assertEquals("昵称只能包含中文、英文字母、数字、下划线和短横线", assertThrows(ServiceException.class,
                () -> loginService.validateNickname("用户 名", true)).getMessage());
        assertEquals("昵称只能包含中文、英文字母、数字、下划线和短横线", assertThrows(ServiceException.class,
                () -> loginService.validateNickname("用户!", true)).getMessage());
        assertEquals("昵称只能包含中文、英文字母、数字、下划线和短横线", assertThrows(ServiceException.class,
                () -> loginService.validateNickname("用户😀", true)).getMessage());
    }

    @Test
    void existingUserNicknameUpdateRejectsNicknameOwnedByAnotherUser()
    {
        WlWxUser existing = user(9L, "旧昵称", "202601/old.png", "0");
        when(userMapper.selectByOpenid("openid-secret")).thenReturn(existing);
        when(userMapper.selectByOpenidForUpdate("openid-secret")).thenReturn(existing);
        when(userMapper.countByNickname("重复昵称", 9L)).thenReturn(1);
        WxLoginRequest request = codeOnly();
        request.setNickname("重复昵称");

        assertEquals("昵称已被使用，请更换后重试", assertThrows(ServiceException.class,
                () -> loginService.login(request, null, null)).getMessage());

        verify(userMapper, never()).updateProfile(any(WlWxUser.class));
    }

    @Test
    void existingUserNicknameUpdateRejectsBlankNicknameInsteadOfIgnoringIt()
    {
        WlWxUser existing = user(9L, "旧昵称", "202601/old.png", "0");
        when(userMapper.selectByOpenid("openid-secret")).thenReturn(existing);
        when(userMapper.selectByOpenidForUpdate("openid-secret")).thenReturn(existing);
        WxLoginRequest request = codeOnly();
        request.setNickname("   ");

        assertEquals("昵称不能为空", assertThrows(ServiceException.class,
                () -> loginService.login(request, null, null)).getMessage());

        verify(userMapper, never()).updateProfile(any(WlWxUser.class));
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
        verify(userMapper).insertDailyActive(9L);
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
        verify(agreementService, never()).acceptCurrent(anyLong(), any(), any());
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

    @Test
    void tokenIsIssuedOnlyAfterDatabaseCommit()
    {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        loginService = new WxLoginService(codeClient, userMapper, agreementService,
                avatarStorageService, tokenService, transactionManager);
        WlWxUser existing = user(9L, "用户", "202601/old.png", "0");
        when(userMapper.selectByOpenid("openid-secret")).thenReturn(existing);
        when(userMapper.selectByOpenidForUpdate("openid-secret")).thenReturn(existing);
        when(agreementService.hasAcceptedAllCurrent(9L)).thenReturn(true);
        when(tokenService.issue(9L)).thenReturn("token");

        loginService.login(codeOnly(), null, null);

        InOrder order = inOrder(transactionManager, tokenService);
        order.verify(transactionManager).commit(transactionStatus);
        order.verify(tokenService).issue(9L);
    }

    @Test
    void commitFailureDeletesFirstLoginAvatarAndNeverIssuesToken()
    {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        doThrow(new TransactionSystemException("commit failed"))
                .when(transactionManager).commit(transactionStatus);
        loginService = new WxLoginService(codeClient, userMapper, agreementService,
                avatarStorageService, tokenService, transactionManager);
        when(avatarStorageService.store(any())).thenReturn("202607/new.jpg");
        doAnswer(invocation -> {
            WlWxUser user = invocation.getArgument(0);
            user.setId(18L);
            return 1;
        }).when(userMapper).insertWxUser(any(WlWxUser.class));

        assertThrows(TransactionSystemException.class,
                () -> loginService.login(completeRequest(), avatar(), "127.0.0.1"));

        verify(avatarStorageService).deleteQuietly("202607/new.jpg");
        verify(tokenService, never()).issue(anyLong());
    }

    @Test
    void replacingAvatarDeletesOldOnlyAfterCommit()
    {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        TransactionStatus transactionStatus = mock(TransactionStatus.class);
        when(transactionManager.getTransaction(any(TransactionDefinition.class))).thenReturn(transactionStatus);
        loginService = new WxLoginService(codeClient, userMapper, agreementService,
                avatarStorageService, tokenService, transactionManager);
        WlWxUser existing = user(9L, "用户", "202601/old.png", "0");
        when(userMapper.selectByOpenid("openid-secret")).thenReturn(existing);
        when(userMapper.selectByOpenidForUpdate("openid-secret")).thenReturn(existing);
        when(avatarStorageService.store(any())).thenReturn("202607/new.png");
        when(agreementService.hasAcceptedAllCurrent(9L)).thenReturn(true);
        when(tokenService.issue(9L)).thenReturn("token");
        WxLoginRequest request = codeOnly();
        request.setNickname("新昵称");

        loginService.login(request, avatar(), null);

        InOrder order = inOrder(transactionManager, avatarStorageService, tokenService);
        order.verify(transactionManager).commit(transactionStatus);
        order.verify(avatarStorageService).deleteQuietly("202601/old.png");
        order.verify(tokenService).issue(9L);
        verify(avatarStorageService, never()).deleteQuietly("202607/new.png");
    }

    private WxLoginRequest completeRequest()
    {
        WxLoginRequest request = codeOnly();
        request.setNickname("测试用户");
        request.setPrivacyAccepted(true);
        request.setPrivacyVersion("privacy-v1");
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
