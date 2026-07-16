package com.ruoyi.library.common;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WxApiResponseTest
{
    @Test
    void successUsesUnifiedSuccessContract()
    {
        WxApiResponse<String> response = WxApiResponse.success("数据");

        assertEquals(0, response.getCode());
        assertEquals("操作成功", response.getMessage());
        assertEquals("数据", response.getData());
    }

    @Test
    void failureKeepsCodeAndMessageWithoutData()
    {
        WxApiResponse<Object> response = WxApiResponse.failure(40101, "登录状态已失效，请重新登录");

        assertEquals(40101, response.getCode());
        assertEquals("登录状态已失效，请重新登录", response.getMessage());
        assertNull(response.getData());
    }
}
