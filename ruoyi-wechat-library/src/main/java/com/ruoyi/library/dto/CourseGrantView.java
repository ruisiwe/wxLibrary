package com.ruoyi.library.dto;

import com.ruoyi.library.domain.WlUserCourse;
import java.util.Date;

/** 当前微信用户的课程码永久授权。 */
public class CourseGrantView
{
    private final Long courseId;
    private final String accessSource;
    private final boolean permanent;
    private final Date grantedTime;

    public CourseGrantView(WlUserCourse grant)
    {
        courseId = grant.getCourseId();
        accessSource = grant.getAccessSource();
        permanent = "1".equals(grant.getPermanent());
        grantedTime = grant.getGrantedTime();
    }

    public Long getCourseId() { return courseId; }
    public String getAccessSource() { return accessSource; }
    public boolean isPermanent() { return permanent; }
    public Date getGrantedTime() { return grantedTime; }
}
