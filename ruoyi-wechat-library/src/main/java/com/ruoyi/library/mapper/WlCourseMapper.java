package com.ruoyi.library.mapper;
import com.ruoyi.library.domain.WlCourse;import com.ruoyi.library.domain.WlCourseVideo;import java.util.List;import org.apache.ibatis.annotations.Param;
/** 课程和视频数据访问。 */
public interface WlCourseMapper
{WlCourse selectById(@Param("id")Long id);List<WlCourse> selectList(WlCourse q);List<WlCourse> selectPublicList();int insertCourse(WlCourse c);int updateCourse(WlCourse c);long countUsage(@Param("courseId")Long id);WlCourseVideo selectVideoById(@Param("id")Long id);List<WlCourseVideo> selectVideos(@Param("courseId")Long id);int insertVideo(WlCourseVideo v);int updateVideo(WlCourseVideo v);}
