package com.ruoyi.web.controller.library.wx;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.common.WxApiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartException;
import org.springframework.web.multipart.support.MissingServletRequestPartException;

/** 小程序接口中文业务异常转换。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ruoyi.web.controller.library.wx")
public class WxApiExceptionHandler
{
    private static final Logger LOGGER = LoggerFactory.getLogger(WxApiExceptionHandler.class);

    /** 将业务异常统一转换为小程序响应结构。 */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<WxApiResponse<Void>> handleServiceException(ServiceException exception)
    {
        int code = exception.getCode() == null ? 40001 : exception.getCode();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(WxApiResponse.failure(code, exception.getMessage()));
    }

    /** 将缺少的请求参数或上传字段转换为统一中文响应。 */
    @ExceptionHandler({MissingServletRequestParameterException.class, MissingServletRequestPartException.class})
    public ResponseEntity<WxApiResponse<Void>> handleMissingParameter(Exception exception)
    {
        return failure(HttpStatus.BAD_REQUEST, 40001, "请求参数不完整");
    }

    /** 将参数绑定和类型转换错误转换为统一中文响应。 */
    @ExceptionHandler({BindException.class, MethodArgumentTypeMismatchException.class,
            MethodArgumentNotValidException.class})
    public ResponseEntity<WxApiResponse<Void>> handleBindingException(Exception exception)
    {
        return failure(HttpStatus.BAD_REQUEST, 40001, "请求参数格式不正确");
    }

    /** 将无法读取的 JSON 内容转换为统一中文响应。 */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<WxApiResponse<Void>> handleUnreadableMessage(HttpMessageNotReadableException exception)
    {
        return failure(HttpStatus.BAD_REQUEST, 40001, "请求内容格式不正确");
    }

    /** 将不支持的媒体类型转换为统一中文响应。 */
    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<WxApiResponse<Void>> handleUnsupportedMediaType(HttpMediaTypeNotSupportedException exception)
    {
        return failure(HttpStatus.UNSUPPORTED_MEDIA_TYPE, 41501, "请求类型不支持");
    }

    /** 将上传大小超限转换为统一中文响应。 */
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<WxApiResponse<Void>> handleUploadLimit(MaxUploadSizeExceededException exception)
    {
        return failure(HttpStatus.PAYLOAD_TOO_LARGE, 41301, "上传文件大小超出限制");
    }

    /** 将 multipart 解析失败转换为统一中文响应。 */
    @ExceptionHandler(MultipartException.class)
    public ResponseEntity<WxApiResponse<Void>> handleMultipartException(MultipartException exception)
    {
        return failure(HttpStatus.BAD_REQUEST, 40001, "上传请求格式不正确");
    }

    /** 隐藏未知异常的内部细节，并返回安全的中文响应。 */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<WxApiResponse<Void>> handleUnknownException(Exception exception)
    {
        LOGGER.error("小程序接口发生未知异常", exception);
        return failure(HttpStatus.INTERNAL_SERVER_ERROR, 50001, "服务暂时不可用，请稍后重试");
    }

    private ResponseEntity<WxApiResponse<Void>> failure(HttpStatus status, int code, String message)
    {
        return ResponseEntity.status(status).body(WxApiResponse.failure(code, message));
    }
}
