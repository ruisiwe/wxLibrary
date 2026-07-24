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
import java.util.Date;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VipCodeServiceTest
{
    private WlVipCodeMapper codeMapper;
    private WlVipPlanMapper planMapper;
    private WlWxUserMapper userMapper;
    private VipEntitlementService entitlementService;
    private VipCodeService service;

    @BeforeEach
    void setUp()
    {
        codeMapper = mock(WlVipCodeMapper.class);
        planMapper = mock(WlVipPlanMapper.class);
        userMapper = mock(WlWxUserMapper.class);
        entitlementService = mock(VipEntitlementService.class);
        service = new VipCodeService(codeMapper, planMapper, userMapper, entitlementService);
    }

    @Test
    void generateStoresOnlyDigestAndMask()
    {
        when(planMapper.selectEnabledById(3L)).thenReturn(plan());
        when(codeMapper.insertCode(any(WlVipCode.class))).thenReturn(1);

        VipCodeBatchResult result = service.generate(3L, 2, null, "admin");

        assertEquals(2, result.getPlaintextCodes().size());
        ArgumentCaptor<WlVipCode> captor = ArgumentCaptor.forClass(WlVipCode.class);
        verify(codeMapper, org.mockito.Mockito.times(2)).insertCode(captor.capture());
        for (WlVipCode row : captor.getAllValues())
        {
            assertEquals(Long.valueOf(3L), row.getPlanId());
            assertEquals("UNUSED", row.getStatus());
            assertEquals(64, row.getCodeDigest().length());
            assertEquals("admin", row.getCreateBy());
            for (String plaintext : result.getPlaintextCodes())
                org.junit.jupiter.api.Assertions.assertFalse(row.getCodeDigest().contains(plaintext));
        }
    }

    @Test
    void redeemCodeOpensVipAndMarksUsed()
    {
        WlVipCode summary = vipCode();
        WlVipCode locked = vipCode();
        WlVipEntitlement entitlement = new WlVipEntitlement();
        entitlement.setId(88L);
        entitlement.setNewExpireTime(new Date(1795305600000L));
        when(userMapper.selectById(11L)).thenReturn(enabledUser());
        when(codeMapper.selectByDigest(any())).thenReturn(summary);
        when(codeMapper.selectByDigestForUpdate(any())).thenReturn(locked);
        when(planMapper.selectEnabledById(3L)).thenReturn(plan());
        when(entitlementService.openOrRenew(eq(11L), any(WlVipPlan.class), eq("VIP_CODE"),
                eq("VIP_CODE:21"))).thenReturn(entitlement);
        when(codeMapper.markUsed(21L, 11L, 88L)).thenReturn(1);

        WlVipEntitlement result = service.redeem(11L, " vip123456789 ");

        assertEquals(88L, result.getId());
        verify(entitlementService).openOrRenew(eq(11L), any(WlVipPlan.class), eq("VIP_CODE"),
                eq("VIP_CODE:21"));
        verify(codeMapper).markUsed(21L, 11L, 88L);
    }

    @Test
    void usedCodeIsRejectedBeforeOpeningVip()
    {
        WlVipCode code = vipCode();
        code.setStatus("USED");
        when(userMapper.selectById(11L)).thenReturn(enabledUser());
        when(codeMapper.selectByDigest(any())).thenReturn(code);
        when(codeMapper.selectByDigestForUpdate(any())).thenReturn(code);

        assertEquals("会员码已被使用", assertThrows(ServiceException.class,
                () -> service.redeem(11L, "VIP123456789")).getMessage());
        verify(entitlementService, never()).openOrRenew(any(), any(), any(), any());
    }

    private WlVipCode vipCode()
    {
        WlVipCode code = new WlVipCode();
        code.setId(21L);
        code.setPlanId(3L);
        code.setStatus("UNUSED");
        code.setBatchNo("VB1");
        return code;
    }

    private WlVipPlan plan()
    {
        WlVipPlan plan = new WlVipPlan();
        plan.setId(3L);
        plan.setPlanCode("MONTH");
        plan.setPlanName("月卡");
        plan.setValidDays(30);
        plan.setGiftPoints(20L);
        plan.setStatus("0");
        return plan;
    }

    private WlWxUser enabledUser()
    {
        WlWxUser user = new WlWxUser();
        user.setId(11L);
        user.setStatus("0");
        return user;
    }
}
