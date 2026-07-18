package com.ruoyi.web.controller.library.wx;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.common.WxApiResponse;
import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.domain.WlCourse;
import com.ruoyi.library.domain.WlUserCourse;
import com.ruoyi.library.domain.WlVideoProgress;
import com.ruoyi.library.dto.CourseGrantView;
import com.ruoyi.library.dto.CourseRedeemRequest;
import com.ruoyi.library.dto.VideoPlaybackView;
import com.ruoyi.library.dto.VideoProgressRequest;
import com.ruoyi.library.mapper.WlUserCourseMapper;
import com.ruoyi.library.service.CourseCodeService;
import com.ruoyi.library.service.CourseService;
import com.ruoyi.library.service.VideoPlaybackService;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** 小程序课程兑换、播放和进度接口。 */
@RestController
@RequestMapping("/wx/courses")
public class WxCourseController
{
    private final CourseCodeService codeService;
    private final CourseService courseService;
    private final VideoPlaybackService playbackService;
    private final WlUserCourseMapper grantMapper;

    public WxCourseController(CourseService courseService, CourseCodeService codeService,
            VideoPlaybackService playbackService, WlUserCourseMapper grantMapper)
    {
        this.courseService = courseService;
        this.codeService = codeService;
        this.playbackService = playbackService;
        this.grantMapper = grantMapper;
    }

    /** 查询启用课程及互斥访问方式，保留登录态客户端兼容路径。 */
    @GetMapping
    public WxApiResponse<List<WlCourse>> list()
    {
        return WxApiResponse.success(courseService.publicList());
    }

    /** 查询课程视频目录，不返回私有对象键，保留登录态客户端兼容路径。 */
    @GetMapping("/{courseId}/videos")
    public WxApiResponse<List<Map<String, Object>>> videos(@PathVariable Long courseId)
    {
        return WxApiResponse.success(courseService.publicVideos(courseId));
    }

    /** 查询当前用户通过课程码获得的永久课程授权。 */
    @GetMapping("/mine")
    public WxApiResponse<List<CourseGrantView>> mine()
    {
        List<CourseGrantView> views = new ArrayList<>();
        for (WlUserCourse grant : grantMapper.selectByUser(WxUserContext.get()))
            views.add(new CourseGrantView(grant));
        return WxApiResponse.success(views);
    }

    /** 使用课程码兑换永久课程权限。 */
    @PostMapping("/redeem")
    public WxApiResponse<WlUserCourse> redeem(@RequestBody CourseRedeemRequest request)
    {
        return WxApiResponse.success(codeService.redeem(WxUserContext.get(),
                request == null ? null : request.getCode()));
    }

    /** 查询当前用户的课程视频播放进度，用于恢复上次播放位置。 */
    @GetMapping("/progress")
    public WxApiResponse<List<WlVideoProgress>> progress()
    {
        return WxApiResponse.success(playbackService.listProgress(WxUserContext.get()));
    }

    /** 获取有权限视频的短时私有播放地址。 */
    @PostMapping("/videos/{videoId}/play")
    public WxApiResponse<VideoPlaybackView> play(@PathVariable Long videoId)
    {
        return WxApiResponse.success(playbackService.play(WxUserContext.get(), videoId));
    }

    /** 保存当前视频播放进度。 */
    @PutMapping("/videos/{videoId}/progress")
    public WxApiResponse<Void> progress(@PathVariable Long videoId,
            @RequestBody VideoProgressRequest request)
    {
        if (request == null || request.getProgressSeconds() == null)
            throw new ServiceException("播放进度不能为空");
        playbackService.saveProgress(WxUserContext.get(), videoId,
                request.getProgressSeconds(), Boolean.TRUE.equals(request.getFinished()));
        return WxApiResponse.success(null);
    }
}
