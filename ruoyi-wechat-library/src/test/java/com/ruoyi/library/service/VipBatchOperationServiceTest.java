package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlVipPlan;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.VipBatchOperationResult;
import com.ruoyi.library.dto.VipOperationRequest;
import com.ruoyi.library.mapper.WlWxUserMapper;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class VipBatchOperationServiceTest
{
    private VipPlanService planService;
    private VipEntitlementService entitlementService;
    private WlWxUserMapper userMapper;
    private VipBatchOperationService service;

    @BeforeEach
    void setUp()
    {
        planService = mock(VipPlanService.class);
        entitlementService = mock(VipEntitlementService.class);
        userMapper = mock(WlWxUserMapper.class);
        service = new VipBatchOperationService(planService, entitlementService, userMapper);
    }

    @Test
    void openDeduplicatesAndSortsUsersAndBuildsPerUserBusinessNumbers()
    {
        WlVipPlan plan = plan();
        when(planService.getEnabled(2L)).thenReturn(plan);
        VipOperationRequest request = request(Arrays.asList(9L, 3L, 9L),
                "A1B2C3D4E5F6G7H8I9J0");
        request.setPlanId(2L);
        request.setReason("线下购买");

        VipBatchOperationResult result = service.open(request, 88L);

        assertEquals(2, result.getProcessedCount());
        InOrder order = inOrder(entitlementService);
        order.verify(entitlementService).openOrRenew(eq(3L), same(plan), eq("MANUAL"),
                eq("MANUAL:A1B2C3D4E5F6G7H8I9J0:3"), eq(88L), eq("线下购买"));
        order.verify(entitlementService).openOrRenew(eq(9L), same(plan), eq("MANUAL"),
                eq("MANUAL:A1B2C3D4E5F6G7H8I9J0:9"), eq(88L), eq("线下购买"));
    }

    @Test
    void compensateUsesCompensationBusinessNumbersAndNoPlan()
    {
        VipOperationRequest request = request(Arrays.asList(5L, 6L),
                "K1L2M3N4O5P6Q7R8S9T0");
        request.setDays(7);

        assertEquals(2, service.compensate(request, 88L).getProcessedCount());

        verify(entitlementService).compensate(5L, 7, 88L, "服务补偿",
                "COMPENSATION:K1L2M3N4O5P6Q7R8S9T0:5");
        verify(entitlementService).compensate(6L, 7, 88L, "服务补偿",
                "COMPENSATION:K1L2M3N4O5P6Q7R8S9T0:6");
    }

    @Test
    void rejectsEmptyInvalidAndTooManyUsers()
    {
        assertMessage("请选择微信用户", () -> service.open(
                request(Collections.emptyList(), "A1B2C3D4E5F6G7H8I9J0"), 88L));
        assertMessage("微信用户编号不正确", () -> service.open(
                request(Arrays.asList(1L, 0L), "A1B2C3D4E5F6G7H8I9J0"), 88L));

        List<Long> userIds = new ArrayList<>();
        for (long id = 1; id <= 101; id++) userIds.add(id);
        assertMessage("一次最多选择100位用户", () -> service.open(
                request(userIds, "A1B2C3D4E5F6G7H8I9J0"), 88L));
    }

    @Test
    void rejectsInvalidBatchReasonAndDays()
    {
        assertMessage("批次编号不正确", () -> service.open(
                request(Collections.singletonList(1L), "short"), 88L));
        VipOperationRequest blankReason = request(Collections.singletonList(1L),
                "A1B2C3D4E5F6G7H8I9J0");
        blankReason.setReason(" ");
        assertMessage("操作原因不能为空", () -> service.open(blankReason, 88L));

        VipOperationRequest invalidDays = request(Collections.singletonList(1L),
                "A1B2C3D4E5F6G7H8I9J0");
        invalidDays.setDays(0);
        assertMessage("补偿天数必须大于0", () -> service.compensate(invalidDays, 88L));
    }

    @Test
    void candidateSearchPassesTrimmedKeywordAndNumericId()
    {
        List<WlWxUser> expected = Collections.singletonList(new WlWxUser());
        when(userMapper.selectVipOperationCandidates("123", 123L)).thenReturn(expected);

        assertEquals(expected, service.userOptions(" 123 "));
        verify(userMapper).selectVipOperationCandidates("123", 123L);
    }

    @Test
    void nonNumericCandidateKeywordDoesNotPassAnId()
    {
        service.userOptions(" 微信 ");

        verify(userMapper).selectVipOperationCandidates("微信", null);
    }

    private VipOperationRequest request(List<Long> userIds, String batchNo)
    {
        VipOperationRequest request = new VipOperationRequest();
        request.setUserIds(userIds);
        request.setBatchNo(batchNo);
        request.setReason("服务补偿");
        return request;
    }

    private WlVipPlan plan()
    {
        WlVipPlan plan = new WlVipPlan();
        plan.setId(2L);
        plan.setStatus("0");
        plan.setValidDays(30);
        plan.setGiftPoints(100L);
        return plan;
    }

    private void assertMessage(String expected, Runnable action)
    {
        assertEquals(expected, assertThrows(ServiceException.class, action::run).getMessage());
    }
}
