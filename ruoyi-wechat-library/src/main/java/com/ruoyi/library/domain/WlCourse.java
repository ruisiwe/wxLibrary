package com.ruoyi.library.domain;
import com.ruoyi.common.core.domain.BaseEntity;
/** 视频课程。 */
public class WlCourse extends BaseEntity
{private static final long serialVersionUID=1L;private Long id;private String title;private String summary;private String coverUrl;private String accessType;private String accessLabel;private Integer sortOrder;private String status;
public Long getId(){return id;}public void setId(Long v){id=v;}public String getTitle(){return title;}public void setTitle(String v){title=v;}public String getSummary(){return summary;}public void setSummary(String v){summary=v;}public String getCoverUrl(){return coverUrl;}public void setCoverUrl(String v){coverUrl=v;}public String getAccessType(){return accessType;}public void setAccessType(String v){accessType=v;}public String getAccessLabel(){return accessLabel;}public void setAccessLabel(String v){accessLabel=v;}public Integer getSortOrder(){return sortOrder;}public void setSortOrder(Integer v){sortOrder=v;}public String getStatus(){return status;}public void setStatus(String v){status=v;}}
