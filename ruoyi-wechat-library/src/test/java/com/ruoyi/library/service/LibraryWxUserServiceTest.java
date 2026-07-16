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
                () -> service.adjustPoints(7L, request(-6L, "manual-1"), "admin"));

        assertEquals("积分余额不能小于0", exception.getMessage());
        verify(userMapper, never()).updatePointBalance(any(), any(), any(), any());
        verify(pointRecordMapper, never()).insertPointRecord(any(WlPointRecord.class));
    }

    @Test
    void adjustmentLocksUserUpdatesBalanceAndWritesSnapshotRecord()
    {
        when(userMapper.selectByIdForUpdate(7L)).thenReturn(user(7L, 10L));
        when(userMapper.updatePointBalance(7L, 10L, 7L, "admin")).thenReturn(1);

        WlPointRecord result = service.adjustPoints(7L, request(-3L, "manual-2"), "admin");

        assertEquals(10L, result.getBeforeBalance());
        assertEquals(7L, result.getAfterBalance());
        assertEquals(-3L, result.getChangePoints());
        ArgumentCaptor<WlPointRecord> captor = ArgumentCaptor.forClass(WlPointRecord.class);
        verify(pointRecordMapper).insertPointRecord(captor.capture());
        assertEquals("manual-2", captor.getValue().getBizNo());
        assertEquals("MANUAL", captor.getValue().getEventType());
    }

    @Test
    void zeroAmountIsRejectedAndExistingBizNumberIsIdempotent()
    {
        assertEquals("积分调整数量不能为0", assertThrows(ServiceException.class,
                () -> service.adjustPoints(7L, request(0L, "manual-3"), "admin")).getMessage());

        WlPointRecord existing = new WlPointRecord();
        existing.setUserId(7L);
        existing.setChangePoints(2L);
        existing.setBizNo("manual-4");
        when(pointRecordMapper.selectByBizNo("manual-4")).thenReturn(existing);
        assertEquals(existing, service.adjustPoints(7L, request(2L, "manual-4"), "admin"));
        verify(userMapper, never()).selectByIdForUpdate(7L);
    }

    @Test
    void rechecksBizNumberAfterUserLockForConcurrentRetry()
    {
        WlPointRecord committed = new WlPointRecord();
        committed.setUserId(7L);
        committed.setChangePoints(2L);
        committed.setBizNo("manual-concurrent");
        when(pointRecordMapper.selectByBizNo("manual-concurrent")).thenReturn(null, committed);
        when(userMapper.selectByIdForUpdate(7L)).thenReturn(user(7L, 10L));

        assertEquals(committed, service.adjustPoints(7L,
                request(2L, "manual-concurrent"), "admin"));

        verify(pointRecordMapper, times(2)).selectByBizNo("manual-concurrent");
        verify(userMapper, never()).updatePointBalance(any(), any(), any(), any());
    }

    private PointAdjustmentRequest request(Long amount, String bizNo)
    {
        PointAdjustmentRequest request = new PointAdjustmentRequest();
        request.setAmount(amount);
        request.setBizNo(bizNo);
        request.setDescription("人工调整");
        return request;
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
