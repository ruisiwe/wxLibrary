package com.ruoyi.library.domain;
import com.ruoyi.common.core.domain.BaseEntity;import java.util.Date;
/** 用户永久课程授权。 */
public class WlUserCourse extends BaseEntity
{private static final long serialVersionUID=1L;private Long id;private Long userId;private Long courseId;private Long courseCodeId;private String accessSource;private String permanent;private Date grantedTime;
public Long getId(){return id;}public void setId(Long v){id=v;}public Long getUserId(){return userId;}public void setUserId(Long v){userId=v;}public Long getCourseId(){return courseId;}public void setCourseId(Long v){courseId=v;}public Long getCourseCodeId(){return courseCodeId;}public void setCourseCodeId(Long v){courseCodeId=v;}public String getAccessSource(){return accessSource;}public void setAccessSource(String v){accessSource=v;}public String getPermanent(){return permanent;}public void setPermanent(String v){permanent=v;}public Date getGrantedTime(){return grantedTime;}public void setGrantedTime(Date v){grantedTime=v;}}
