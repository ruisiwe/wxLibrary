package com.ruoyi.library.mapper;
import com.ruoyi.library.domain.WlCourseCode;import java.util.List;import org.apache.ibatis.annotations.Param;
/** 课程兑换码数据访问。 */
public interface WlCourseCodeMapper
{WlCourseCode selectByDigest(@Param("digest")String d);WlCourseCode selectByDigestForUpdate(@Param("digest")String d);List<WlCourseCode> selectList(WlCourseCode q);int insertCode(WlCourseCode c);int markUsed(@Param("id")Long id,@Param("userId")Long userId);}
