package com.ruoyi.library.agreement;

import java.util.Collections;
import java.util.Date;
import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlAgreement;
import com.ruoyi.library.domain.WlUserAgreement;
import com.ruoyi.library.mapper.WlAgreementMapper;
import com.ruoyi.library.mapper.WlUserAgreementMapper;
import com.ruoyi.library.dto.FileDisclaimerDto;
import com.ruoyi.library.dto.OriginalFileRequest;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 微信用户协议服务。 */
@Service
public class WxAgreementService
{
    public static final String TYPE_PRIVACY = "PRIVACY";
    public static final String TYPE_STATEMENT = "STATEMENT";
    public static final String TYPE_FILE_DISCLAIMER = "FILE_DISCLAIMER";
    public static final String STATUS_DRAFT = "0";
    public static final String STATUS_PUBLISHED = "1";

    private final WlAgreementMapper agreementMapper;
    private final WlUserAgreementMapper userAgreementMapper;

    public WxAgreementService(WlAgreementMapper agreementMapper, WlUserAgreementMapper userAgreementMapper)
    {
        this.agreementMapper = agreementMapper;
        this.userAgreementMapper = userAgreementMapper;
    }

    /** 查询当前生效的用户隐私协议。 */
    public List<WlAgreement> current()
    {
        return Collections.singletonList(requiredCurrent(TYPE_PRIVACY));
    }

    /** 校验客户端提交的隐私协议版本是否为当前版本。 */
    public void validateCurrentAcceptance(boolean privacyAccepted, String privacyVersion)
    {
        WlAgreement privacy = requiredCurrent(TYPE_PRIVACY);
        if (!privacyAccepted) throw new ServiceException("请勾选用户隐私协议");
        if (!privacy.getVersion().equals(trim(privacyVersion)))
            throw new ServiceException("请提交当前用户隐私协议版本");
    }

    /** 幂等确认当前生效的用户隐私协议。 */
    @Transactional
    public void acceptCurrent(Long userId, String privacyVersion, String acceptedIp)
    {
        WlAgreement privacy = requiredCurrent(TYPE_PRIVACY);
        if (!privacy.getVersion().equals(trim(privacyVersion)))
            throw new ServiceException("请提交当前用户隐私协议版本");
        acceptOne(userId, privacy, acceptedIp);
    }

    public boolean hasAcceptedAllCurrent(Long userId)
    {
        WlAgreement privacy = requiredCurrent(TYPE_PRIVACY);
        return userAgreementMapper.countAcceptedAgreementId(userId, privacy.getId()) == 1;
    }

    /** 查询当前文件发送免责声明及当前用户是否已免提示。 */
    public FileDisclaimerDto fileDisclaimer(Long userId)
    {
        if (userId == null || userId <= 0) throw new ServiceException("微信用户身份无效");
        WlAgreement agreement = requiredFileDisclaimer();
        FileDisclaimerDto result = new FileDisclaimerDto();
        result.setAgreementId(agreement.getId());
        result.setAgreementVersion(agreement.getVersion());
        result.setTitle(agreement.getTitle());
        result.setContent(agreement.getContent());
        result.setReminderSuppressed(userAgreementMapper.selectByUserAndAgreement(
                userId, agreement.getId()) != null);
        return result;
    }

    /** 校验本次确认；仅勾选以后不再提示时持久化当前版本。 */
    @Transactional
    public void validateFileDisclaimer(Long userId, OriginalFileRequest request, String acceptedIp)
    {
        if (userId == null || userId <= 0) throw new ServiceException("微信用户身份无效");
        WlAgreement current = requiredFileDisclaimer();
        if (userAgreementMapper.selectByUserAndAgreement(userId, current.getId()) != null) return;
        if (request == null || !current.getId().equals(request.getAgreementId())
                || !current.getVersion().equals(trim(request.getAgreementVersion())))
            throw new ServiceException("文件发送免责声明已更新，请重新确认");
        if (!request.isConfirmed()) throw new ServiceException("请确认文件发送免责声明");
        if (request.isReminderSuppressed()) acceptOne(userId, current, acceptedIp);
    }

    public List<WlAgreement> list(WlAgreement query) { return agreementMapper.selectAgreementList(query); }
    public WlAgreement detail(Long id) { return agreementMapper.selectAgreementById(id); }

    public int addDraft(WlAgreement agreement, String operator)
    {
        validateAgreement(agreement);
        agreement.setStatus(STATUS_DRAFT);
        agreement.setCreateBy(operator);
        return agreementMapper.insertAgreement(agreement);
    }

    public int updateDraft(WlAgreement agreement)
    {
        WlAgreement stored = agreementMapper.selectAgreementById(agreement.getId());
        if (stored == null) throw new ServiceException("协议不存在");
        if (!STATUS_DRAFT.equals(stored.getStatus())) throw new ServiceException("仅草稿协议允许编辑");
        validateAgreement(agreement);
        return agreementMapper.updateDraft(agreement);
    }

    /** 发布草稿并停用同类型旧版本。 */
    @Transactional
    public void publish(Long id, String operator)
    {
        WlAgreement draft = agreementMapper.selectAgreementByIdForUpdate(id);
        if (draft == null) throw new ServiceException("协议不存在");
        if (STATUS_PUBLISHED.equals(draft.getStatus())) return;
        if (!STATUS_DRAFT.equals(draft.getStatus()))
            throw new ServiceException("该协议已被后续版本替代，不能重复发布");
        if (draft.getEffectiveTime() == null) throw new ServiceException("协议生效时间不能为空");
        if (!draft.getEffectiveTime().after(new Date()))
            agreementMapper.disablePublishedByType(draft.getAgreementType(), id, operator);
        if (agreementMapper.publishAgreement(id, operator) != 1) throw new ServiceException("协议发布失败");
    }

    private void acceptOne(Long userId, WlAgreement agreement, String acceptedIp)
    {
        if (userAgreementMapper.selectByUserAndAgreement(userId, agreement.getId()) != null) return;
        WlUserAgreement accepted = new WlUserAgreement();
        accepted.setUserId(userId);
        accepted.setAgreementId(agreement.getId());
        accepted.setAgreementType(agreement.getAgreementType());
        accepted.setAgreementVersion(agreement.getVersion());
        accepted.setAcceptedTime(new Date());
        accepted.setAcceptedIp(acceptedIp);
        try { userAgreementMapper.insertUserAgreement(accepted); }
        catch (DuplicateKeyException ignored) { /* 并发重复确认按幂等成功处理。 */ }
    }

    private WlAgreement requiredCurrent(String type)
    {
        WlAgreement agreement = agreementMapper.selectCurrentByType(type);
        if (agreement == null) throw new ServiceException("当前协议尚未发布，请联系管理员");
        return agreement;
    }

    private WlAgreement requiredFileDisclaimer()
    {
        WlAgreement agreement = agreementMapper.selectCurrentByType(TYPE_FILE_DISCLAIMER);
        if (agreement == null) throw new ServiceException("文件发送免责声明暂未发布，请联系客服");
        return agreement;
    }

    private void validateAgreement(WlAgreement agreement)
    {
        if (agreement == null || (!TYPE_PRIVACY.equals(agreement.getAgreementType())
                && !TYPE_STATEMENT.equals(agreement.getAgreementType())
                && !TYPE_FILE_DISCLAIMER.equals(agreement.getAgreementType())))
            throw new ServiceException("协议类型不正确");
        if (isBlank(agreement.getVersion()) || isBlank(agreement.getTitle()) || isBlank(agreement.getContent()))
            throw new ServiceException("协议版本、标题和内容不能为空");
        if (agreement.getEffectiveTime() == null) throw new ServiceException("协议生效时间不能为空");
    }

    private String trim(String value) { return value == null ? null : value.trim(); }
    private boolean isBlank(String value) { return value == null || value.trim().isEmpty(); }
}
