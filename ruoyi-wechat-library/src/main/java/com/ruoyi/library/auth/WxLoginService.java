package com.ruoyi.library.auth;

import java.util.Date;
import java.util.function.Supplier;
import java.util.regex.Pattern;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.agreement.WxAgreementService;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.WxLoginRequest;
import com.ruoyi.library.dto.WxLoginResponse;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.storage.AvatarStorageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.multipart.MultipartFile;

/** 独立微信用户登录服务。 */
@Service
public class WxLoginService
{
    private static final Pattern CONTROL_OR_HTML = Pattern.compile("[<>\\p{Cc}\\p{Cf}]");
    private final WechatCodeClient codeClient;
    private final WlWxUserMapper userMapper;
    private final WxAgreementService agreementService;
    private final AvatarStorageService avatarStorageService;
    private final WxTokenService tokenService;
    private final TransactionTemplate transactionTemplate;

    public WxLoginService(WechatCodeClient codeClient, WlWxUserMapper userMapper,
            WxAgreementService agreementService, AvatarStorageService avatarStorageService,
            WxTokenService tokenService)
    {
        this.codeClient = codeClient;
        this.userMapper = userMapper;
        this.agreementService = agreementService;
        this.avatarStorageService = avatarStorageService;
        this.tokenService = tokenService;
        this.transactionTemplate = null;
    }

    @Autowired
    public WxLoginService(WechatCodeClient codeClient, WlWxUserMapper userMapper,
            WxAgreementService agreementService, AvatarStorageService avatarStorageService,
            WxTokenService tokenService, PlatformTransactionManager transactionManager)
    {
        this.codeClient = codeClient;
        this.userMapper = userMapper;
        this.agreementService = agreementService;
        this.avatarStorageService = avatarStorageService;
        this.tokenService = tokenService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    /** 登录并签发与若依后台完全独立的小程序令牌。 */
    public WxLoginResponse login(WxLoginRequest request, MultipartFile avatar, String acceptedIp)
    {
        if (request == null) throw new ServiceException("登录参数不能为空");
        String openid = codeClient.exchange(request.getCode());
        AvatarMutation mutation = new AvatarMutation();
        LoginState state;
        try
        {
            state = executeInTransaction(() -> loginInTransaction(openid, request, avatar, acceptedIp, mutation));
        }
        catch (RuntimeException exception)
        {
            if (mutation.newAvatar != null) avatarStorageService.deleteQuietly(mutation.newAvatar);
            throw exception;
        }
        if (mutation.replaceOld && mutation.oldAvatar != null)
            avatarStorageService.deleteQuietly(mutation.oldAvatar);
        String token = tokenService.issue(state.user.getId());
        return new WxLoginResponse(token, state.user.getId(), state.user.getNickname(), state.user.getAvatarPath(),
                state.user.getPointBalance(), state.agreementRequired);
    }

    private LoginState loginInTransaction(String openid, WxLoginRequest request, MultipartFile avatar,
            String acceptedIp, AvatarMutation mutation)
    {
        WlWxUser existing = userMapper.selectByOpenid(openid);
        WlWxUser user;
        if (existing == null)
        {
            user = createFirstUser(openid, request, avatar, acceptedIp, mutation);
        }
        else
        {
            user = userMapper.selectByOpenidForUpdate(openid);
            if (user == null) throw new ServiceException("微信用户状态已变化，请重新登录");
            ensureEnabled(user);
            updateExistingProfile(user, request, avatar, mutation);
            if (request.hasAgreementSubmission())
            {
                agreementService.validateCurrentAcceptance(request.isPrivacyAccepted(), request.getPrivacyVersion(),
                        request.isStatementAccepted(), request.getStatementVersion());
                agreementService.acceptCurrent(user.getId(), request.getPrivacyVersion(),
                        request.getStatementVersion(), acceptedIp);
            }
            userMapper.updateLastLoginTime(user.getId());
        }
        boolean agreementRequired = !agreementService.hasAcceptedAllCurrent(user.getId());
        return new LoginState(user, agreementRequired);
    }

    private WlWxUser createFirstUser(String openid, WxLoginRequest request, MultipartFile avatar,
            String acceptedIp, AvatarMutation mutation)
    {
        if (avatar == null || avatar.isEmpty()) throw new ServiceException("首次登录必须上传有效头像");
        String nickname = validateNickname(request.getNickname(), true);
        agreementService.validateCurrentAcceptance(request.isPrivacyAccepted(), request.getPrivacyVersion(),
                request.isStatementAccepted(), request.getStatementVersion());
        String avatarPath = avatarStorageService.store(avatar);
        mutation.newAvatar = avatarPath;
        WlWxUser created = new WlWxUser();
        created.setOpenid(openid);
        created.setNickname(nickname);
        created.setAvatarPath(avatarPath);
        created.setPointBalance(0L);
        created.setStatus("0");
        created.setLastLoginTime(new Date());
        created.setCreateBy("wx");
        try
        {
            userMapper.insertWxUser(created);
        }
        catch (DuplicateKeyException exception)
        {
            avatarStorageService.deleteQuietly(avatarPath);
            mutation.newAvatar = null;
            WlWxUser concurrent = userMapper.selectByOpenidForUpdate(openid);
            if (concurrent == null) throw exception;
            ensureEnabled(concurrent);
            userMapper.updateLastLoginTime(concurrent.getId());
            if (request.hasAgreementSubmission())
                agreementService.acceptCurrent(concurrent.getId(), request.getPrivacyVersion(),
                        request.getStatementVersion(), acceptedIp);
            return concurrent;
        }
        agreementService.acceptCurrent(created.getId(), request.getPrivacyVersion(),
                request.getStatementVersion(), acceptedIp);
        return created;
    }

    private void updateExistingProfile(WlWxUser user, WxLoginRequest request, MultipartFile avatar,
            AvatarMutation mutation)
    {
        boolean changed = false;
        if (request.getNickname() != null && !request.getNickname().trim().isEmpty())
        {
            user.setNickname(validateNickname(request.getNickname(), true));
            changed = true;
        }
        if (avatar != null && !avatar.isEmpty())
        {
            String oldAvatar = user.getAvatarPath();
            String newAvatar = avatarStorageService.store(avatar);
            mutation.oldAvatar = oldAvatar;
            mutation.newAvatar = newAvatar;
            mutation.replaceOld = !newAvatar.equals(oldAvatar);
            user.setAvatarPath(newAvatar);
            changed = true;
        }
        if (changed)
        {
            user.setUpdateBy("wx");
            userMapper.updateProfile(user);
        }
    }

    public String validateNickname(String nickname, boolean required)
    {
        String value = nickname == null ? "" : nickname.trim();
        if (value.isEmpty())
        {
            if (required) throw new ServiceException("首次登录必须填写昵称");
            return value;
        }
        if (value.length() > 64) throw new ServiceException("昵称长度不能超过64个字符");
        if (CONTROL_OR_HTML.matcher(value).find()) throw new ServiceException("昵称不能包含HTML标签或控制字符");
        return value;
    }

    private void ensureEnabled(WlWxUser user)
    {
        if (!"0".equals(user.getStatus())) throw new ServiceException("该微信用户已被停用，无法登录");
    }

    private <T> T executeInTransaction(Supplier<T> callback)
    {
        if (transactionTemplate == null) return callback.get();
        return transactionTemplate.execute(status -> callback.get());
    }

    private static class LoginState
    {
        private final WlWxUser user;
        private final boolean agreementRequired;

        private LoginState(WlWxUser user, boolean agreementRequired)
        {
            this.user = user;
            this.agreementRequired = agreementRequired;
        }
    }

    private static class AvatarMutation
    {
        private String oldAvatar;
        private String newAvatar;
        private boolean replaceOld;
    }
}
