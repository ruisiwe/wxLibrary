package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.PointAdjustmentRequest;
import com.ruoyi.library.mapper.WlPointRecordMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

class LibraryWxUserServiceTest
{
    private WlWxUserMapper userMapper;
    private WlPointRecordMapper pointRecordMapper;
    private LibraryWxUserService service;

    @BeforeEach
    void setUp()
    {
        userMapper = mock(WlWxUserMapper.class);
        pointRecordMapper = mock(WlPointRecordMapper.class);
        service = new LibraryWxUserService(userMapper, pointRecordMapper);
    }

    @Test
    void deductionCannotMakeBalanceNegative()
    {
        when(userMapper.selectByIdForUpdate(7L)).thenReturn(user(7L, 5L));

        ServiceException exception = assertThrows(ServiceException.class,
                () -> service.adjustPoints(7L,
                        request(-6L, "A1B2C3D4E5F6G7H8I9J0"), "admin"));

        assertEquals("积分余额不能小于0", exception.getMessage());
        verify(userMapper, never()).updatePointBalance(any(), any(), any(), any());
        verify(pointRecordMapper, never()).insertPointRecord(any(WlPointRecord.class));
    }

    @Test
    void adjustmentLocksUserUpdatesBalanceAndWritesSnapshotRecord()
    {
        when(userMapper.selectByIdForUpdate(7L)).thenReturn(user(7L, 10L));
        when(userMapper.updatePointBalance(7L, 10L, 7L, "admin")).thenReturn(1);

        WlPointRecord result = service.adjustPoints(7L,
                request(-3L, "A1B2C3D4E5F6G7H8I9J0"), "admin");

        assertEquals(10L, result.getBeforeBalance());
        assertEquals(7L, result.getAfterBalance());
        assertEquals(-3L, result.getChangePoints());
        ArgumentCaptor<WlPointRecord> captor = ArgumentCaptor.forClass(WlPointRecord.class);
        verify(pointRecordMapper).insertPointRecord(captor.capture());
        assertEquals("MANUAL_POINT:A1B2C3D4E5F6G7H8I9J0:7",
                captor.getValue().getBizNo());
        assertEquals("MANUAL", captor.getValue().getEventType());
        assertEquals("人工调整", captor.getValue().getDescription());
    }

    @Test
    void invalidRequestFieldsAreRejected()
    {
        assertMessage("积分调整请求不能为空",
                () -> service.adjustPoints(7L, null, "admin"));
        assertMessage("微信用户编号不正确",
                () -> service.adjustPoints(0L,
                        request(1L, "A1B2C3D4E5F6G7H8I9J0"), "admin"));
        assertMessage("积分调整数量不能为0",
                () -> service.adjustPoints(7L,
                        request(0L, "A1B2C3D4E5F6G7H8I9J0"), "admin"));
        assertMessage("积分调整批次编号不正确",
                () -> service.adjustPoints(7L, request(1L, "short"), "admin"));

        PointAdjustmentRequest blankReason =
                request(1L, "A1B2C3D4E5F6G7H8I9J0");
        blankReason.setDescription(" ");
        assertMessage("积分调整原因不能为空",
                () -> service.adjustPoints(7L, blankReason, "admin"));

        PointAdjustmentRequest longReason =
                request(1L, "A1B2C3D4E5F6G7H8I9J0");
        longReason.setDescription(String.join("",
                java.util.Collections.nCopies(201, "原")));
        assertMessage("积分调整原因不能超过200个字符",
                () -> service.adjustPoints(7L, longReason, "admin"));
    }

    @Test
    void matchingManualAdjustmentIsIdempotent()
    {
        WlPointRecord existing = new WlPointRecord();
        existing.setUserId(7L);
        existing.setChangePoints(2L);
        existing.setEventType("MANUAL");
        existing.setBizNo("MANUAL_POINT:A1B2C3D4E5F6G7H8I9J0:7");
        when(pointRecordMapper.selectByBizNo(existing.getBizNo())).thenReturn(existing);

        assertEquals(existing, service.adjustPoints(7L,
                request(2L, "A1B2C3D4E5F6G7H8I9J0"), "admin"));
        verify(userMapper, never()).selectByIdForUpdate(7L);
    }

    @Test
    void businessNumberUsedByAnotherOperationIsRejected()
    {
        WlPointRecord existing = new WlPointRecord();
        existing.setUserId(7L);
        existing.setChangePoints(2L);
        existing.setEventType("SIGN");
        existing.setBizNo("MANUAL_POINT:A1B2C3D4E5F6G7H8I9J0:7");
        when(pointRecordMapper.selectByBizNo(existing.getBizNo())).thenReturn(existing);

        assertMessage("积分调整业务编号已被其他操作使用",
                () -> service.adjustPoints(7L,
                        request(2L, "A1B2C3D4E5F6G7H8I9J0"), "admin"));
        verify(userMapper, never()).selectByIdForUpdate(7L);
    }

    @Test
    void rechecksBizNumberAfterUserLockForConcurrentRetry()
    {
        WlPointRecord committed = new WlPointRecord();
        committed.setUserId(7L);
        committed.setChangePoints(2L);
        committed.setEventType("MANUAL");
        committed.setBizNo("MANUAL_POINT:J1K2L3M4N5P6Q7R8S9T0:7");
        when(pointRecordMapper.selectByBizNo(committed.getBizNo())).thenReturn(null, committed);
        when(userMapper.selectByIdForUpdate(7L)).thenReturn(user(7L, 10L));

        assertEquals(committed, service.adjustPoints(7L,
                request(2L, "J1K2L3M4N5P6Q7R8S9T0"), "admin"));

        verify(pointRecordMapper, times(2)).selectByBizNo(committed.getBizNo());
        verify(userMapper, never()).updatePointBalance(any(), any(), any(), any());
    }

    private PointAdjustmentRequest request(Long amount, String batchNo)
    {
        PointAdjustmentRequest request = new PointAdjustmentRequest();
        request.setAmount(amount);
        request.setBatchNo(batchNo);
        request.setDescription("人工调整");
        return request;
    }

    private void assertMessage(String expected, Runnable action)
    {
        assertEquals(expected,
                assertThrows(ServiceException.class, action::run).getMessage());
    }

    private WlWxUser user(Long id, Long points)
    {
        WlWxUser user = new WlWxUser();
        user.setId(id);
        user.setPointBalance(points);
        user.setStatus("0");
        return user;
    }
}
