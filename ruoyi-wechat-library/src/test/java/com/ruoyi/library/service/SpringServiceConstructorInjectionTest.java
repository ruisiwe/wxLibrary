package com.ruoyi.library.service;

import com.ruoyi.library.mapper.WlCourseMapper;
import com.ruoyi.library.mapper.WlPointMapper;
import com.ruoyi.library.mapper.WlPointRecordMapper;
import com.ruoyi.library.mapper.WlUserCourseMapper;
import com.ruoyi.library.mapper.WlVideoProgressMapper;
import com.ruoyi.library.mapper.WlVipEntitlementMapper;
import com.ruoyi.library.mapper.WlWxUserMapper;
import com.ruoyi.library.storage.PrivateFileUrlSigner;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

class SpringServiceConstructorInjectionTest
{
    @Test
    void createsPointServiceThroughItsProductionConstructor()
    {
        assertServiceCanBeCreated(PointService.class,
                mock(WlPointMapper.class), mock(WlPointRecordMapper.class), mock(WlWxUserMapper.class));
    }

    @Test
    void createsVipEntitlementServiceThroughItsProductionConstructor()
    {
        assertServiceCanBeCreated(VipEntitlementService.class,
                mock(WlVipEntitlementMapper.class), mock(WlWxUserMapper.class), mock(PointService.class));
    }

    @Test
    void createsVideoPlaybackServiceThroughItsProductionConstructor()
    {
        assertServiceCanBeCreated(VideoPlaybackService.class,
                mock(WlCourseMapper.class), mock(WlUserCourseMapper.class), mock(WlWxUserMapper.class),
                mock(WlVideoProgressMapper.class), mock(PrivateFileUrlSigner.class));
    }

    private void assertServiceCanBeCreated(Class<?> serviceClass, Object... dependencies)
    {
        try (AnnotationConfigApplicationContext context = new AnnotationConfigApplicationContext())
        {
            for (int index = 0; index < dependencies.length; index++)
            {
                context.getBeanFactory().registerSingleton("dependency" + index, dependencies[index]);
            }
            context.register(serviceClass);
            context.refresh();
            assertNotNull(context.getBean(serviceClass));
        }
    }
}
