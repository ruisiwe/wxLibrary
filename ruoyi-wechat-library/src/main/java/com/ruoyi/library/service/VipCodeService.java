package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipCode;
import com.ruoyi.library.domain.WlVipEntitlement;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.VipCodeBatchResult;
import com.ruoyi.library.mapper.WlVipCodeMapper;
import com.ruoyi.library.mapper.WlVipPlanMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 会员兑换码批量生成和兑换服务。 */
@Service
public class VipCodeService
{
    private static final char[] ALPHABET = "23456789ABCDEFGHJKLMNPQRSTUVWXYZ".toCharArray();
    private static final int CODE_LENGTH = 16;

    private final WlVipCodeMapper codeMapper;
    private final WlVipPlanMapper planMapper;
    private final WlWxUserMapper userMapper;
    private final VipEntitlementService entitlementService;
    private final SecureRandom random = new SecureRandom();

    public VipCodeService(WlVipCodeMapper codeMapper, WlVipPlanMapper planMapper,
            WlWxUserMapper userMapper, VipEntitlementService entitlementService)
    {
        this.codeMapper = codeMapper;
        this.planMapper = planMapper;
        this.userMapper = userMapper;
        this.entitlementService = entitlementService;
    }

    @Transactional
    public VipCodeBatchResult generate(Long planId, int count, Date expires, String operator)
    {
        requireEnabledPlan(planId);
        if (count < 1 || count > 1000) throw new ServiceException("单批会员码数量必须在1到1000之间");
        String batch = "VB" + UUID.randomUUID().toString().replace("-", "");
        List<String> plaintext = new ArrayList<>();
        for (int index = 0; index < count; index++)
        {
            String code = nextUniqueCode();
            WlVipCode row = new WlVipCode();
            row.setPlanId(planId);
            row.setCodeDigest(digest(code));
            row.setCodeMask("****" + code.substring(code.length() - 4));
            row.setStatus("UNUSED");
            row.setExpiresTime(expires);
            row.setBatchNo(batch);
            row.setCreateBy(operator);
            if (codeMapper.insertCode(row) != 1) throw new ServiceException("会员码生成失败，请重试");
            plaintext.add(code);
        }
        return new VipCodeBatchResult(batch, plaintext);
    }

    @Transactional
    public WlVipEntitlement redeem(Long userId, String plaintext)
    {
        requireEnabledUser(userMapper.selectById(userId));
        String hash = digest(normalize(plaintext));
        WlVipCode summary = codeMapper.selectByDigest(hash);
        if (summary == null) throw new ServiceException("会员码不存在");
        WlVipCode code = codeMapper.selectByDigestForUpdate(hash);
        if (code == null) throw new ServiceException("会员码不存在");
        if (!"UNUSED".equals(code.getStatus())) throw new ServiceException("会员码已被使用");
        if (code.getExpiresTime() != null && !code.getExpiresTime().after(new Date()))
            throw new ServiceException("会员码已过期");
        WlVipPlan plan = requireEnabledPlan(code.getPlanId());
        WlVipEntitlement entitlement = entitlementService.openOrRenew(userId, plan,
                "VIP_CODE", "VIP_CODE:" + code.getId());
        if (codeMapper.markUsed(code.getId(), userId, entitlement.getId()) != 1)
            throw new ServiceException("会员码兑换失败，请重试");
        return entitlement;
    }

    public List<WlVipCode> list(WlVipCode query)
    {
        return codeMapper.selectList(query == null ? new WlVipCode() : query);
    }

    public int disable(Long id, String operator)
    {
        if (id == null || id <= 0) throw new ServiceException("会员码编号不能为空");
        int rows = codeMapper.disable(id, operator);
        if (rows != 1) throw new ServiceException("会员码不存在或状态不可修改");
        return rows;
    }

    private WlVipPlan requireEnabledPlan(Long planId)
    {
        WlVipPlan plan = planMapper.selectEnabledById(planId);
        if (plan == null) throw new ServiceException("会员套餐不存在或已停用");
        return plan;
    }

    private void requireEnabledUser(WlWxUser user)
    {
        if (user == null) throw new ServiceException("微信用户不存在");
        if (!"0".equals(user.getStatus())) throw new ServiceException("当前账号已停用，请联系管理员");
    }

    private String nextUniqueCode()
    {
        for (int count = 0; count < 10; count++)
        {
            StringBuilder builder = new StringBuilder(CODE_LENGTH);
            for (int index = 0; index < CODE_LENGTH; index++)
                builder.append(ALPHABET[random.nextInt(ALPHABET.length)]);
            String code = builder.toString();
            if (codeMapper.selectByDigest(digest(code)) == null) return code;
        }
        throw new ServiceException("会员码生成冲突，请重试");
    }

    private String normalize(String value)
    {
        if (value == null || value.trim().isEmpty()) throw new ServiceException("会员码不能为空");
        String normalized = value.trim().toUpperCase(Locale.ROOT);
        if (normalized.length() < 8 || normalized.length() > 32)
            throw new ServiceException("会员码格式不正确");
        return normalized;
    }

    private String digest(String value)
    {
        try
        {
            byte[] bytes = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(64);
            for (byte current : bytes) builder.append(String.format("%02x", current & 255));
            return builder.toString();
        }
        catch (NoSuchAlgorithmException exception)
        {
            throw new IllegalStateException("系统不支持 SHA-256", exception);
        }
    }
}
