package com.ruoyi.library.auth;

import java.util.Date;
import java.util.regex.Pattern;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.agreement.WxAgreementService;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.WxLoginRequest;
import com.ruoyi.library.dto.WxLoginResponse;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.storage.AvatarStorageService;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
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

    public WxLoginService(WechatCodeClient codeClient, WlWxUserMapper userMapper,
            WxAgreementService agreementService, AvatarStorageService avatarStorageService,
            WxTokenService tokenService)
    {
        this.codeClient = codeClient;
        this.userMapper = userMapper;
        this.agreementService = agreementService;
        this.avatarStorageService = avatarStorageService;
        this.tokenService = tokenService;
    }

    /** 登录并签发与若依后台完全独立的小程序令牌。 */
    @Transactional
    public WxLoginResponse login(WxLoginRequest request, MultipartFile avatar, String acceptedIp)
    {
        if (request == null) throw new ServiceException("登录参数不能为空");
        String openid = codeClient.exchange(request.getCode());
        WlWxUser existing = userMapper.selectByOpenid(openid);
        WlWxUser user;
        if (existing == null)
        {
            user = createFirstUser(openid, request, avatar, acceptedIp);
        }
        else
        {
            user = userMapper.selectByOpenidForUpdate(openid);
            if (user == null) throw new ServiceException("微信用户状态已变化，请重新登录");
            ensureEnabled(user);
            updateExistingProfile(user, request, avatar);
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
        String token = tokenService.issue(user.getId());
        return new WxLoginResponse(token, user.getId(), user.getNickname(), user.getAvatarPath(),
                user.getPointBalance(), agreementRequired);
    }

    private WlWxUser createFirstUser(String openid, WxLoginRequest request, MultipartFile avatar, String acceptedIp)
    {
        if (avatar == null || avatar.isEmpty()) throw new ServiceException("首次登录必须上传有效头像");
        String nickname = validateNickname(request.getNickname(), true);
        agreementService.validateCurrentAcceptance(request.isPrivacyAccepted(), request.getPrivacyVersion(),
                request.isStatementAccepted(), request.getStatementVersion());
        String avatarPath = avatarStorageService.store(avatar);
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

    private void updateExistingProfile(WlWxUser user, WxLoginRequest request, MultipartFile avatar)
    {
        boolean changed = false;
        if (request.getNickname() != null && !request.getNickname().trim().isEmpty())
        {
            user.setNickname(validateNickname(request.getNickname(), true));
            changed = true;
        }
        if (avatar != null && !avatar.isEmpty())
        {
            user.setAvatarPath(avatarStorageService.store(avatar));
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
}
