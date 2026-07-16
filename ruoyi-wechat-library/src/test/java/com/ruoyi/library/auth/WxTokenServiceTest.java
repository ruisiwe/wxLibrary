package com.ruoyi.library.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;
import com.ruoyi.common.core.redis.RedisCache;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class WxTokenServiceTest
{
    private RedisCache redisCache;
    private WxTokenService tokenService;

    @BeforeEach
    void setUp()
    {
        redisCache = mock(RedisCache.class);
        tokenService = new WxTokenService(redisCache);
    }

    @Test
    void issueCreatesLowercaseHexTokenAndStoresOnlyItsDigest()
            throws Exception
    {
        String token = tokenService.issue(27L);

        assertTrue(token.matches("[0-9a-f]{64}"));
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(redisCache).setCacheObject(keyCaptor.capture(), org.mockito.ArgumentMatchers.eq(27L),
                org.mockito.ArgumentMatchers.eq(30), org.mockito.ArgumentMatchers.eq(TimeUnit.DAYS));
        assertEquals("wx:token:" + sha256(token), keyCaptor.getValue());
        assertFalse(keyCaptor.getValue().contains(token));
    }

    @Test
    void issueRejectsNullUserIdBeforeWritingRedis()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tokenService.issue(null));

        assertEquals("微信用户编号必须为正数", exception.getMessage());
        verify(redisCache, never()).setCacheObject(anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(TimeUnit.class));
    }

    @Test
    void issueRejectsZeroUserIdBeforeWritingRedis()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tokenService.issue(0L));

        assertEquals("微信用户编号必须为正数", exception.getMessage());
        verify(redisCache, never()).setCacheObject(anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(TimeUnit.class));
    }

    @Test
    void issueRejectsNegativeUserIdBeforeWritingRedis()
    {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> tokenService.issue(-1L));

        assertEquals("微信用户编号必须为正数", exception.getMessage());
        verify(redisCache, never()).setCacheObject(anyString(), org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyInt(), org.mockito.ArgumentMatchers.any(TimeUnit.class));
    }

    @Test
    void resolveReturnsUserAndRefreshesThirtyDayTtl()
            throws Exception
    {
        String token = "1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef";
        String key = "wx:token:" + sha256(token);
        when(redisCache.getCacheObject(key)).thenReturn(88L);

        assertEquals(88L, tokenService.resolve(token));
        verify(redisCache).expire(key, 30, TimeUnit.DAYS);
    }

    @Test
    void resolveReturnsNullForBlankOrMissingTokenWithoutRefreshing()
    {
        assertNull(tokenService.resolve(null));
        assertNull(tokenService.resolve("  "));
        when(redisCache.getCacheObject(anyString())).thenReturn(null);
        assertNull(tokenService.resolve("abcdef"));

        verify(redisCache, never()).expire(anyString(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(TimeUnit.class));
    }

    @Test
    void resolveDeletesZeroUserIdWithoutRefreshing()
            throws Exception
    {
        String token = "zero-user-token";
        String key = "wx:token:" + sha256(token);
        when(redisCache.getCacheObject(key)).thenReturn(0L);

        assertNull(tokenService.resolve(token));

        verify(redisCache).deleteObject(key);
        verify(redisCache, never()).expire(anyString(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(TimeUnit.class));
    }

    @Test
    void resolveDeletesNegativeUserIdWithoutRefreshing()
            throws Exception
    {
        String token = "negative-user-token";
        String key = "wx:token:" + sha256(token);
        when(redisCache.getCacheObject(key)).thenReturn(-1L);

        assertNull(tokenService.resolve(token));

        verify(redisCache).deleteObject(key);
        verify(redisCache, never()).expire(anyString(), org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(TimeUnit.class));
    }

    @Test
    void revokeDeletesDigestKeyOnly()
            throws Exception
    {
        String token = "abcdef";

        tokenService.revoke(token);

        verify(redisCache).deleteObject("wx:token:" + sha256(token));
    }

    @Test
    void blankRevokeDoesNothing()
    {
        tokenService.revoke(" ");

        verify(redisCache, never()).deleteObject(anyString());
    }

    private String sha256(String value) throws Exception
    {
        byte[] digest = MessageDigest.getInstance("SHA-256")
                .digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder(digest.length * 2);
        for (byte item : digest)
        {
            result.append(String.format("%02x", item & 0xff));
        }
        return result.toString();
    }
}
