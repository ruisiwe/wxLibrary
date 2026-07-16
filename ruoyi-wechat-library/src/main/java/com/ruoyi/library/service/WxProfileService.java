package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.auth.WxLoginService;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.WxProfileResponse;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.storage.AvatarStorageService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

/** 小程序用户资料服务。 */
@Service
public class WxProfileService
{
    private final WlWxUserMapper userMapper;
    private final AvatarStorageService avatarStorageService;
    private final WxLoginService loginService;

    public WxProfileService(WlWxUserMapper userMapper, AvatarStorageService avatarStorageService,
            WxLoginService loginService)
    {
        this.userMapper = userMapper;
        this.avatarStorageService = avatarStorageService;
        this.loginService = loginService;
    }

    public WxProfileResponse get(Long userId)
    {
        WlWxUser user = userMapper.selectById(userId);
        if (user == null) throw new ServiceException("微信用户不存在");
        return new WxProfileResponse(user);
    }

    /** 修改当前用户昵称和头像，不接受 openid 字段。 */
    @Transactional
    public WxProfileResponse update(Long userId, String nickname, MultipartFile avatar)
    {
        WlWxUser user = userMapper.selectByIdForUpdate(userId);
        if (user == null) throw new ServiceException("微信用户不存在");
        if (!"0".equals(user.getStatus())) throw new ServiceException("该微信用户已被停用，无法修改资料");
        String oldAvatar = user.getAvatarPath();
        String newAvatar = null;
        if (nickname != null) user.setNickname(loginService.validateNickname(nickname, true));
        if (avatar != null && !avatar.isEmpty())
        {
            newAvatar = avatarStorageService.store(avatar);
            user.setAvatarPath(newAvatar);
        }
        if (nickname == null && newAvatar == null) throw new ServiceException("请提交需要修改的昵称或头像");
        user.setUpdateBy("wx");
        try
        {
            if (userMapper.updateProfile(user) != 1) throw new ServiceException("用户资料修改失败");
        }
        catch (RuntimeException exception)
        {
            if (newAvatar != null) avatarStorageService.deleteQuietly(newAvatar);
            throw exception;
        }
        scheduleAvatarCleanup(oldAvatar, newAvatar);
        return new WxProfileResponse(user);
    }

    private void scheduleAvatarCleanup(String oldAvatar, String newAvatar)
    {
        if (newAvatar == null || newAvatar.equals(oldAvatar)) return;
        if (!TransactionSynchronizationManager.isSynchronizationActive())
        {
            if (oldAvatar != null) avatarStorageService.deleteQuietly(oldAvatar);
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
        {
            @Override
            public void afterCommit()
            {
                if (oldAvatar != null) avatarStorageService.deleteQuietly(oldAvatar);
            }

            @Override
            public void afterCompletion(int status)
            {
                if (status != TransactionSynchronization.STATUS_COMMITTED)
                    avatarStorageService.deleteQuietly(newAvatar);
            }
        });
    }
}
