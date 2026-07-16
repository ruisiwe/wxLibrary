package com.ruoyi.library.common;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class WxUserContextTest
{
    @AfterEach
    void tearDown()
    {
        WxUserContext.clear();
    }

    @Test
    void storesAndClearsIndependentWechatUserId()
    {
        WxUserContext.set(12L);
        assertEquals(12L, WxUserContext.get());

        WxUserContext.clear();
        assertNull(WxUserContext.get());
    }

    @Test
    void nullValueDoesNotRemainInThreadLocal()
    {
        WxUserContext.set(12L);
        WxUserContext.set(null);

        assertNull(WxUserContext.get());
    }
}
