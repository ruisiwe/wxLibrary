package com.ruoyi.library.mapper;

import com.ruoyi.library.domain.WlUserCourse;
import java.util.List;
import org.apache.ibatis.annotations.Param;

/** 用户永久课程授权数据访问。 */
public interface WlUserCourseMapper
{
    WlUserCourse selectByUserCourse(@Param("userId") Long userId,
            @Param("courseId") Long courseId);

    List<WlUserCourse> selectByUser(@Param("userId") Long userId);

    int insertGrant(WlUserCourse grant);
}
