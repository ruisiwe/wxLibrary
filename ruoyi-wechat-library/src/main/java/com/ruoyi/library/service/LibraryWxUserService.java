package com.ruoyi.library.service;

import java.util.List;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlPointRecord;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.dto.LibraryWxUserView;
import com.ruoyi.library.dto.PointAdjustmentRequest;
import com.ruoyi.library.mapper.WlPointRecordMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 后台微信用户管理服务。 */
@Service
public class LibraryWxUserService
{
    private final WlWxUserMapper userMapper;
    private final WlPointRecordMapper pointRecordMapper;

    public LibraryWxUserService(WlWxUserMapper userMapper, WlPointRecordMapper pointRecordMapper)
    {
        this.userMapper = userMapper;
        this.pointRecordMapper = pointRecordMapper;
    }

    public List<WlWxUser> list(WlWxUser query) { return userMapper.selectWxUserList(query); }

    public LibraryWxUserView detail(Long id)
    {
        WlWxUser user = userMapper.selectById(id);
        if (user == null) throw new ServiceException("微信用户不存在");
        return LibraryWxUserView.from(user);
    }

    public int changeStatus(Long id, String status, String operator)
    {
        if (!"0".equals(status) && !"1".equals(status)) throw new ServiceException("用户状态不正确");
        if (userMapper.selectById(id) == null) throw new ServiceException("微信用户不存在");
        return userMapper.updateStatus(id, status, operator);
    }

    /** 人工调整积分并记录前后余额快照。 */
    @Transactional
    public WlPointRecord adjustPoints(Long userId, PointAdjustmentRequest request, String operator)
    {
        if (request == null) throw new ServiceException("积分调整请求不能为空");
        if (userId == null || userId <= 0) throw new ServiceException("微信用户编号不正确");
        if (request.getAmount() == null || request.getAmount() == 0)
            throw new ServiceException("积分调整数量不能为0");
        if (request.getBatchNo() == null
                || !request.getBatchNo().matches("[A-Za-z0-9]{20}"))
            throw new ServiceException("积分调整批次编号不正确");
        if (request.getDescription() == null || request.getDescription().trim().isEmpty())
            throw new ServiceException("积分调整原因不能为空");
        String description = request.getDescription().trim();
        if (description.length() > 200)
            throw new ServiceException("积分调整原因不能超过200个字符");
        String bizNo = "MANUAL_POINT:" + request.getBatchNo() + ":" + userId;
        WlPointRecord existing = pointRecordMapper.selectByBizNo(bizNo);
        if (existing != null) return requireMatchingAdjustment(existing, userId, request.getAmount());
        WlWxUser user = userMapper.selectByIdForUpdate(userId);
        if (user == null) throw new ServiceException("微信用户不存在");
        // 用户行锁会串行化同一用户的并发调整，加锁后必须再次检查幂等流水。
        existing = pointRecordMapper.selectByBizNo(bizNo);
        if (existing != null) return requireMatchingAdjustment(existing, userId, request.getAmount());
        long before = user.getPointBalance() == null ? 0L : user.getPointBalance();
        long after;
        try { after = Math.addExact(before, request.getAmount()); }
        catch (ArithmeticException exception) { throw new ServiceException("积分调整数量超出范围"); }
        if (after < 0) throw new ServiceException("积分余额不能小于0");
        if (userMapper.updatePointBalance(userId, before, after, operator) != 1)
            throw new ServiceException("积分余额已变化，请重试");
        WlPointRecord record = new WlPointRecord();
        record.setUserId(userId);
        record.setEventType("MANUAL");
        record.setBizNo(bizNo);
        record.setChangePoints(request.getAmount());
        record.setBeforeBalance(before);
        record.setAfterBalance(after);
        record.setDescription(description);
        record.setCreateBy(operator);
        pointRecordMapper.insertPointRecord(record);
        return record;
    }

    private WlPointRecord requireMatchingAdjustment(WlPointRecord existing,
            Long userId, Long amount)
    {
        if (!userId.equals(existing.getUserId())
                || !amount.equals(existing.getChangePoints())
                || !"MANUAL".equals(existing.getEventType()))
            throw new ServiceException("积分调整业务编号已被其他操作使用");
        return existing;
    }
}
