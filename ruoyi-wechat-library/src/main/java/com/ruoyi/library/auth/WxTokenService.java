package com.ruoyi.library.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.concurrent.TimeUnit;
import org.springframework.stereotype.Service;
import com.ruoyi.common.core.redis.RedisCache;

/**
 * 小程序独立登录令牌服务，不复用若依后台令牌。
 */
@Service
public class WxTokenService
{
    private static final String TOKEN_KEY_PREFIX = "wx:token:";
    private static final int TOKEN_BYTES = 32;
    private static final int TOKEN_TTL_DAYS = 30;

    private final RedisCache redisCache;
    private final SecureRandom secureRandom = new SecureRandom();

    public WxTokenService(RedisCache redisCache)
    {
        this.redisCache = redisCache;
    }

    /**
     * 为微信用户签发独立令牌。
     *
     * @param userId 微信用户编号
     * @return 仅返回给客户端一次的令牌明文
     */
    public String issue(Long userId)
    {
        if (userId == null || userId <= 0)
        {
            throw new IllegalArgumentException("微信用户编号必须为正数");
        }
        byte[] randomBytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomBytes);
        String token = toHex(randomBytes);
        redisCache.setCacheObject(buildCacheKey(token), userId, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        return token;
    }

    /**
     * 解析令牌并刷新滑动有效期。
     *
     * @param token 客户端令牌
     * @return 微信用户编号，令牌无效时返回空
     */
    public Long resolve(String token)
    {
        if (isBlank(token))
        {
            return null;
        }
        String key = buildCacheKey(token);
        Long userId = redisCache.getCacheObject(key);
        if (userId == null)
        {
            return null;
        }
        if (userId <= 0)
        {
            redisCache.deleteObject(key);
            return null;
        }
        redisCache.expire(key, TOKEN_TTL_DAYS, TimeUnit.DAYS);
        return userId;
    }

    /**
     * 撤销令牌。
     *
     * @param token 客户端令牌
     */
    public void revoke(String token)
    {
        if (!isBlank(token))
        {
            redisCache.deleteObject(buildCacheKey(token));
        }
    }

    private String buildCacheKey(String token)
    {
        return TOKEN_KEY_PREFIX + sha256(token);
    }

    private String sha256(String value)
    {
        try
        {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return toHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        }
        catch (NoSuchAlgorithmException exception)
        {
            throw new IllegalStateException("当前运行环境不支持安全摘要算法", exception);
        }
    }

    private String toHex(byte[] bytes)
    {
        char[] hex = "0123456789abcdef".toCharArray();
        char[] result = new char[bytes.length * 2];
        for (int index = 0; index < bytes.length; index++)
        {
            int value = bytes[index] & 0xff;
            result[index * 2] = hex[value >>> 4];
            result[index * 2 + 1] = hex[value & 0x0f];
        }
        return new String(result);
    }

    private boolean isBlank(String value)
    {
        return value == null || value.trim().isEmpty();
    }
}
