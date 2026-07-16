package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlPointRecord;
import org.apache.ibatis.annotations.Param;

/** 积分流水数据访问。 */
public interface WlPointRecordMapper
{
    WlPointRecord selectByBizNo(@Param("bizNo") String bizNo);
    int insertPointRecord(WlPointRecord record);
}
