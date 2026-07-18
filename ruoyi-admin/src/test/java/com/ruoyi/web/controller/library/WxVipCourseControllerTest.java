package com.ruoyi.web.controller.library;

import com.ruoyi.library.common.WxUserContext;
import com.ruoyi.library.domain.WlCourse;
import com.ruoyi.library.domain.WlVipOrder;
import com.ruoyi.library.mapper.WlUserCourseMapper;
import com.ruoyi.library.service.CourseCodeService;
import com.ruoyi.library.service.CourseService;
import com.ruoyi.library.service.VideoPlaybackService;
import com.ruoyi.library.service.VipOrderService;
import com.ruoyi.library.service.VipPlanService;
import com.ruoyi.web.controller.library.wx.WxApiExceptionHandler;
import com.ruoyi.web.controller.library.wx.WxCourseController;
import com.ruoyi.web.controller.library.wx.WxPublicCourseController;
import com.ruoyi.web.controller.library.wx.WxVipController;
import java.util.Collections;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class WxVipCourseControllerTest
{
    private CourseService courseService;
    private VipOrderService orderService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp()
    {
        courseService = mock(CourseService.class);
        orderService = mock(VipOrderService.class);
        mockMvc = MockMvcBuilders.standaloneSetup(
                        new WxPublicCourseController(courseService),
                        new WxCourseController(courseService, mock(CourseCodeService.class),
                                mock(VideoPlaybackService.class), mock(WlUserCourseMapper.class)),
                        new WxVipController(mock(VipPlanService.class), orderService))
                .setControllerAdvice(new WxApiExceptionHandler())
                .build();
        WxUserContext.set(11L);
    }

    @AfterEach
    void tearDown() { WxUserContext.clear(); }

    @Test
    void publicCourseListContainsOnlyPublicMetadata() throws Exception
    {
        WlCourse course = new WlCourse();
        course.setId(3L);
        course.setTitle("会员课程");
        course.setAccessType("VIP");
        course.setAccessLabel("VIP 可看");
        when(courseService.publicList()).thenReturn(Collections.singletonList(course));

        mockMvc.perform(get("/wx/public/courses"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].title").value("会员课程"))
                .andExpect(jsonPath("$.data[0].accessType").value("VIP"))
                .andExpect(jsonPath("$.data[0].videoObjectKey").doesNotExist());
    }

    @Test
    void paymentStatusReturnsOwnedOrderWithoutTransactionIdentifier() throws Exception
    {
        WlVipOrder order = new WlVipOrder();
        order.setUserId(11L);
        order.setMerchantOrderNo("VIP-1");
        order.setWechatTransactionId("secret-transaction");
        order.setOrderStatus("PAID");
        when(orderService.getForUser(11L, "VIP-1")).thenReturn(order);

        mockMvc.perform(get("/wx/vip/orders/status/VIP-1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.merchantOrderNo").value("VIP-1"))
                .andExpect(jsonPath("$.data.orderStatus").value("PAID"))
                .andExpect(jsonPath("$.data.userId").doesNotExist())
                .andExpect(jsonPath("$.data.wechatTransactionId").doesNotExist());
    }
}
