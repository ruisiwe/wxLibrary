package com.ruoyi.library.common;

/**
 * 当前请求的微信用户上下文，与若依后台用户体系完全独立。
 */
public final class WxUserContext
{
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    private WxUserContext()
    {
    }

    public static void set(Long userId)
    {
        if (userId == null)
        {
            clear();
            return;
        }
        USER_ID.set(userId);
    }

    public static Long get()
    {
        return USER_ID.get();
    }

    public static void clear()
    {
        USER_ID.remove();
    }
}
