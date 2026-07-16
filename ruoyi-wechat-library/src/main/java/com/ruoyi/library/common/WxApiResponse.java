package com.ruoyi.library.common;

/**
 * 小程序接口统一响应。
 *
 * @param <T> 数据类型
 */
public final class WxApiResponse<T>
{
    private final int code;
    private final String message;
    private final T data;

    private WxApiResponse(int code, String message, T data)
    {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 创建成功响应。
     *
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 成功响应
     */
    public static <T> WxApiResponse<T> success(T data)
    {
        return new WxApiResponse<>(0, "操作成功", data);
    }

    /**
     * 创建失败响应。
     *
     * @param code 业务错误码
     * @param message 中文错误信息
     * @param <T> 数据类型
     * @return 失败响应
     */
    public static <T> WxApiResponse<T> failure(int code, String message)
    {
        return new WxApiResponse<>(code, message, null);
    }

    public int getCode()
    {
        return code;
    }

    public String getMessage()
    {
        return message;
    }

    public T getData()
    {
        return data;
    }
}
