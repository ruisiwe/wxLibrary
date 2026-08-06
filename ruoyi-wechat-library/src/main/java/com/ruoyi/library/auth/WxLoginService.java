package com.ruoyi.library.auth;

import java.security.SecureRandom;
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
    private static final Pattern NICKNAME_WHITELIST =
            Pattern.compile("^[\\p{IsHan}A-Za-z0-9_-]+$");
    private static final int MAX_NICKNAME_LENGTH = 20;
    private static final char[] RANDOM_NICKNAME_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final int RANDOM_NICKNAME_LENGTH = 10;
    private static final int MAX_RANDOM_NICKNAME_ATTEMPTS = 10;
    private static final SecureRandom RANDOM = new SecureRandom();
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
                agreementService.validateCurrentAcceptance(request.isPrivacyAccepted(), request.getPrivacyVersion());
                agreementService.acceptCurrent(user.getId(), request.getPrivacyVersion(), acceptedIp);
            }
            userMapper.updateLastLoginTime(user.getId());
        }
        userMapper.insertDailyActive(user.getId());
        boolean agreementRequired = !agreementService.hasAcceptedAllCurrent(user.getId());
        return new LoginState(user, agreementRequired);
    }

    private WlWxUser createFirstUser(String openid, WxLoginRequest request, MultipartFile avatar,
            String acceptedIp, AvatarMutation mutation)
    {
        if (avatar == null || avatar.isEmpty()) throw new ServiceException("首次登录必须上传有效头像");
        String avatarPath = avatarStorageService.store(avatar);
        mutation.newAvatar = avatarPath;
        WlWxUser created = new WlWxUser();
        created.setOpenid(openid);
        created.setAvatarPath(avatarPath);
        created.setPointBalance(0L);
        created.setStatus("0");
        created.setLastLoginTime(new Date());
        created.setCreateBy("wx");
        for (int attempt = 0; attempt < MAX_RANDOM_NICKNAME_ATTEMPTS; attempt++)
        {
            String nickname = generateRandomNickname();
            if (userMapper.countByNickname(nickname, null) > 0) continue;
            created.setNickname(nickname);
            try
            {
                userMapper.insertWxUser(created);
                return created;
            }
            catch (DuplicateKeyException exception)
            {
                WlWxUser concurrent = userMapper.selectByOpenidForUpdate(openid);
                if (concurrent == null) continue;
                avatarStorageService.deleteQuietly(avatarPath);
                mutation.newAvatar = null;
                ensureEnabled(concurrent);
                userMapper.updateLastLoginTime(concurrent.getId());
                if (request.hasAgreementSubmission())
                    agreementService.acceptCurrent(concurrent.getId(), request.getPrivacyVersion(), acceptedIp);
                return concurrent;
            }
        }
        avatarStorageService.deleteQuietly(avatarPath);
        mutation.newAvatar = null;
        throw new ServiceException("系统生成昵称失败，请稍后重试");
    }

    private void updateExistingProfile(WlWxUser user, WxLoginRequest request, MultipartFile avatar,
            AvatarMutation mutation)
    {
        boolean changed = false;
        if (request.getNickname() != null)
        {
            user.setNickname(validateUniqueNickname(request.getNickname(), user.getId()));
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
            try
            {
                userMapper.updateProfile(user);
            }
            catch (DuplicateKeyException exception)
            {
                throw new ServiceException("昵称已被使用，请更换后重试");
            }
        }
    }

    public String validateNickname(String nickname, boolean required)
    {
        String value = nickname == null ? "" : nickname.trim();
        if (value.isEmpty())
        {
            if (required) throw new ServiceException("昵称不能为空");
            return value;
        }
        if (value.codePointCount(0, value.length()) > MAX_NICKNAME_LENGTH)
            throw new ServiceException("昵称长度不能超过20个字符");
        if ("null".equalsIgnoreCase(value) || "undefined".equalsIgnoreCase(value))
            throw new ServiceException("昵称不能使用保留名称");
        if (!NICKNAME_WHITELIST.matcher(value).matches())
            throw new ServiceException("昵称只能包含中文、英文字母、数字、下划线和短横线");
        return value;
    }

    public String validateUniqueNickname(String nickname, Long currentUserId)
    {
        String value = validateNickname(nickname, true);
        if (userMapper.countByNickname(value, currentUserId) > 0)
            throw new ServiceException("昵称已被使用，请更换后重试");
        return value;
    }

    private String generateRandomNickname()
    {
        char[] value = new char[RANDOM_NICKNAME_LENGTH];
        for (int index = 0; index < value.length; index++)
        {
            value[index] = RANDOM_NICKNAME_ALPHABET[RANDOM.nextInt(RANDOM_NICKNAME_ALPHABET.length)];
        }
        return new String(value);
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
