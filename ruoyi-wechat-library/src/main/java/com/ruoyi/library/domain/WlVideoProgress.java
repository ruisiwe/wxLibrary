package com.ruoyi.library.domain;
import com.ruoyi.common.core.domain.BaseEntity;import java.util.Date;
/** 用户视频播放进度。 */
public class WlVideoProgress extends BaseEntity
{private static final long serialVersionUID=1L;private Long id;private Long userId;private Long courseId;private Long videoId;private Integer progressSeconds;private String finished;private Date lastPlayTime;
public Long getId(){return id;}public void setId(Long v){id=v;}public Long getUserId(){return userId;}public void setUserId(Long v){userId=v;}public Long getCourseId(){return courseId;}public void setCourseId(Long v){courseId=v;}public Long getVideoId(){return videoId;}public void setVideoId(Long v){videoId=v;}public Integer getProgressSeconds(){return progressSeconds;}public void setProgressSeconds(Integer v){progressSeconds=v;}public String getFinished(){return finished;}public void setFinished(String v){finished=v;}public Date getLastPlayTime(){return lastPlayTime;}public void setLastPlayTime(Date v){lastPlayTime=v;}}
