package com.ruoyi.web.controller.library.wx;
import com.ruoyi.common.exception.ServiceException;import com.ruoyi.library.common.*;import com.ruoyi.library.domain.*;import com.ruoyi.library.dto.*;import com.ruoyi.library.service.*;import java.util.*;import org.springframework.web.bind.annotation.*;
/** 小程序课程列表、兑换、播放和进度接口。 */
@RestController @RequestMapping("/wx/courses") public class WxCourseController
{private final CourseService courseService;private final CourseCodeService codeService;private final VideoPlaybackService playbackService;public WxCourseController(CourseService c,CourseCodeService x,VideoPlaybackService p){courseService=c;codeService=x;playbackService=p;}
/** 查询启用课程及访问方式提示。 */@GetMapping public WxApiResponse<List<WlCourse>> list(){return WxApiResponse.success(courseService.publicList());}
/** 查询课程视频目录，不返回私有对象键。 */@GetMapping("/{courseId}/videos") public WxApiResponse<List<Map<String,Object>>> videos(@PathVariable Long courseId){return WxApiResponse.success(courseService.publicVideos(courseId));}
/** 使用课程码兑换永久课程权限。 */@PostMapping("/redeem") public WxApiResponse<WlUserCourse> redeem(@RequestBody CourseRedeemRequest r){return WxApiResponse.success(codeService.redeem(WxUserContext.get(),r==null?null:r.getCode()));}
/** 获取有权限视频的短时私有播放地址。 */@PostMapping("/videos/{videoId}/play") public WxApiResponse<VideoPlaybackView> play(@PathVariable Long videoId){return WxApiResponse.success(playbackService.play(WxUserContext.get(),videoId));}
/** 保存当前视频播放进度。 */@PutMapping("/videos/{videoId}/progress") public WxApiResponse<Void> progress(@PathVariable Long videoId,@RequestBody VideoProgressRequest r){if(r==null||r.getProgressSeconds()==null)throw new ServiceException("播放进度不能为空");playbackService.saveProgress(WxUserContext.get(),videoId,r.getProgressSeconds(),Boolean.TRUE.equals(r.getFinished()));return WxApiResponse.success(null);}}
