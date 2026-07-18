package com.ruoyi.web.controller.library.wx;

import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.domain.WlCourse;
import com.ruoyi.library.service.CourseService;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序匿名课程元数据接口，不返回私有视频对象键。 */
@RestController
@RequestMapping("/wx/public/courses")
public class WxPublicCourseController
{
    private final CourseService courseService;

    public WxPublicCourseController(CourseService courseService)
    {
        this.courseService = courseService;
    }

    /** 匿名查询已启用的会员课程与课程码课程。 */
    @GetMapping
    public WxApiResponse<List<WlCourse>> list()
    {
        return WxApiResponse.success(courseService.publicList());
    }

    /** 匿名查询已启用课程的视频目录，不返回私有对象键和播放地址。 */
    @GetMapping("/{courseId}/videos")
    public WxApiResponse<List<Map<String, Object>>> videos(@PathVariable Long courseId)
    {
        return WxApiResponse.success(courseService.publicVideos(courseId));
    }
}
