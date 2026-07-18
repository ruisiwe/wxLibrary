package com.ruoyi.library.domain;
import com.ruoyi.common.core.domain.BaseEntity;
/** 课程私有视频。 */
public class WlCourseVideo extends BaseEntity
{private static final long serialVersionUID=1L;private Long id;private Long courseId;private String title;private String videoObjectKey;private Integer durationSeconds;private Integer sortOrder;private String status;
public Long getId(){return id;}public void setId(Long v){id=v;}public Long getCourseId(){return courseId;}public void setCourseId(Long v){courseId=v;}public String getTitle(){return title;}public void setTitle(String v){title=v;}public String getVideoObjectKey(){return videoObjectKey;}public void setVideoObjectKey(String v){videoObjectKey=v;}public Integer getDurationSeconds(){return durationSeconds;}public void setDurationSeconds(Integer v){durationSeconds=v;}public Integer getSortOrder(){return sortOrder;}public void setSortOrder(Integer v){sortOrder=v;}public String getStatus(){return status;}public void setStatus(String v){status=v;}}
