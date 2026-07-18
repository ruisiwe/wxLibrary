package com.ruoyi.library.mapper;
import com.ruoyi.library.domain.WlVideoProgress;import java.util.List;import org.apache.ibatis.annotations.Param;
/** 视频进度数据访问。 */
public interface WlVideoProgressMapper
{int upsertProgress(WlVideoProgress p);List<WlVideoProgress> selectByUser(@Param("userId")Long userId);}
