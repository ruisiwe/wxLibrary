package com.ruoyi.library.service;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.domain.WlCourse;
import com.ruoyi.library.domain.WlCourseCode;
import com.ruoyi.library.domain.WlCourseVideo;
import com.ruoyi.library.domain.WlUserCourse;
import com.ruoyi.library.domain.WlWxUser;
import com.ruoyi.library.mapper.WlCourseCodeMapper;
import com.ruoyi.library.mapper.WlCourseMapper;
import com.ruoyi.library.mapper.WlUserCourseMapper;
import com.ruoyi.library.mapper.WlVideoProgressMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.storage.PrivateFileUrlSigner;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CourseAccessServiceTest
{
    @Test
    void generatedCodesProvideAtLeastEightyBitsAndNeverPersistPlaintext()
    {
        WlCourseCodeMapper codeMapper=mock(WlCourseCodeMapper.class);WlCourseMapper courseMapper=mock(WlCourseMapper.class);
        when(courseMapper.selectById(3L)).thenReturn(course(3L,"CODE"));when(codeMapper.insertCode(any())).thenReturn(1);
        CourseCodeService service=new CourseCodeService(codeMapper,mock(WlUserCourseMapper.class),courseMapper,mock(WlWxUserMapper.class));
        com.ruoyi.library.dto.CourseCodeBatchResult result=service.generate(3L,20,null,"admin");
        assertEquals(20,result.getPlaintextCodes().size());assertEquals(20,new java.util.HashSet<>(result.getPlaintextCodes()).size());
        for(String code:result.getPlaintextCodes())assertTrue(code.matches("[23456789ABCDEFGHJKLMNPQRSTUVWXYZ]{16}"));
    }
    @Test
    void accessTypeMustBeVipOrCodeAndCannotChangeAfterUse()
    {
        WlCourseMapper mapper=mock(WlCourseMapper.class);CourseService service=new CourseService(mapper);
        WlCourse invalid=course(1L,"FREE");
        assertThrows(ServiceException.class,()->service.add(invalid,"admin"));
        WlCourse changed=course(1L,"VIP");when(mapper.selectById(1L)).thenReturn(course(1L,"CODE"));
        when(mapper.countUsage(1L)).thenReturn(1L);
        assertThrows(ServiceException.class,()->service.edit(changed,"admin"));
        verify(mapper,never()).updateCourse(any());
    }

    @Test
    void oneCodeCanBeConsumedByOneWechatUserOnly()
    {
        WlCourseCodeMapper codeMapper=mock(WlCourseCodeMapper.class);WlUserCourseMapper grantMapper=mock(WlUserCourseMapper.class);
        WlCourseMapper courseMapper=mock(WlCourseMapper.class);WlWxUserMapper userMapper=mock(WlWxUserMapper.class);
        WlCourseCode code=new WlCourseCode();code.setId(8L);code.setCourseId(3L);code.setStatus("UNUSED");
        when(codeMapper.selectByDigest(any())).thenReturn(code);when(codeMapper.selectByDigestForUpdate(any())).thenReturn(code);when(courseMapper.selectById(3L)).thenReturn(course(3L,"CODE"));
        when(userMapper.selectById(1L)).thenReturn(user(1L));when(userMapper.selectById(2L)).thenReturn(user(2L));
        when(grantMapper.insertGrant(any())).thenReturn(1);when(codeMapper.markUsed(8L,1L)).thenAnswer(i->{code.setStatus("USED");code.setUsedUserId(1L);return 1;});
        CourseCodeService service=new CourseCodeService(codeMapper,grantMapper,courseMapper,userMapper);
        service.redeem(1L,"ABCDEFGHJKLMNPQR");
        assertThrows(ServiceException.class,()->service.redeem(2L,"ABCDEFGHJKLMNPQR"));
    }

    @Test
    void existingOwnerLeavesSubmittedCodeUnused()
    {
        WlCourseCodeMapper codeMapper=mock(WlCourseCodeMapper.class);WlUserCourseMapper grantMapper=mock(WlUserCourseMapper.class);
        WlCourseMapper courseMapper=mock(WlCourseMapper.class);WlWxUserMapper userMapper=mock(WlWxUserMapper.class);
        WlCourseCode code=new WlCourseCode();code.setId(8L);code.setCourseId(3L);code.setStatus("UNUSED");when(codeMapper.selectByDigest(any())).thenReturn(code);
        WlUserCourse existing=new WlUserCourse();existing.setId(9L);existing.setUserId(1L);existing.setCourseId(3L);when(grantMapper.selectByUserCourse(1L,3L)).thenReturn(existing);
        when(userMapper.selectById(1L)).thenReturn(user(1L));
        CourseCodeService service=new CourseCodeService(codeMapper,grantMapper,courseMapper,userMapper);
        assertTrue(service.redeem(1L,"ABCDEFGHJKLMNPQR")==existing);verify(codeMapper,never()).markUsed(any(),any());
    }

    @Test
    void codeCourseRemainsPlayableAfterVipExpiryButVipCourseDoesNot()
    {
        WlCourseMapper courseMapper=mock(WlCourseMapper.class);WlUserCourseMapper grantMapper=mock(WlUserCourseMapper.class);
        WlWxUserMapper userMapper=mock(WlWxUserMapper.class);WlVideoProgressMapper progressMapper=mock(WlVideoProgressMapper.class);
        WlCourseVideo video=new WlCourseVideo();video.setId(5L);video.setCourseId(3L);video.setStatus("0");
        when(courseMapper.selectVideoById(5L)).thenReturn(video);WlWxUser expired=user(1L);expired.setVipExpireTime(Date.from(Instant.parse("2026-07-17T00:00:00Z")));when(userMapper.selectById(1L)).thenReturn(expired);
        when(courseMapper.selectById(3L)).thenReturn(course(3L,"CODE"));when(grantMapper.selectByUserCourse(1L,3L)).thenReturn(new WlUserCourse());
        VideoPlaybackService service=new VideoPlaybackService(courseMapper,grantMapper,userMapper,progressMapper,mock(PrivateFileUrlSigner.class),Clock.fixed(Instant.parse("2026-07-18T00:00:00Z"),ZoneOffset.UTC));
        assertTrue(service.canPlay(1L,5L));
        when(courseMapper.selectById(3L)).thenReturn(course(3L,"VIP"));when(grantMapper.selectByUserCourse(1L,3L)).thenReturn(null);
        assertFalse(service.canPlay(1L,5L));
    }
    private WlCourse course(Long id,String type){WlCourse c=new WlCourse();c.setId(id);c.setTitle("课程");c.setAccessType(type);c.setStatus("0");return c;}
    private WlWxUser user(Long id){WlWxUser u=new WlWxUser();u.setId(id);u.setStatus("0");return u;}
}
