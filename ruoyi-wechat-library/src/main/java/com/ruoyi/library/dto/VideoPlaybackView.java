package com.ruoyi.library.dto;
/** 私有视频短时播放地址。 */
public class VideoPlaybackView
{private final Long videoId;private final String playUrl;private final long expiresInSeconds;public VideoPlaybackView(Long i,String u,long e){videoId=i;playUrl=u;expiresInSeconds=e;}public Long getVideoId(){return videoId;}public String getPlayUrl(){return playUrl;}public long getExpiresInSeconds(){return expiresInSeconds;}}
