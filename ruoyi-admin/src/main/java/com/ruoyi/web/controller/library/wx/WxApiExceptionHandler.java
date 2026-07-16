package com.ruoyi.web.controller.library.wx;

import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.library.common.WxApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** 小程序接口中文业务异常转换。 */
@Order(Ordered.HIGHEST_PRECEDENCE)
@RestControllerAdvice(basePackages = "com.ruoyi.web.controller.library.wx")
public class WxApiExceptionHandler
{
    /** 将业务异常统一转换为小程序响应结构。 */
    @ExceptionHandler(ServiceException.class)
    public ResponseEntity<WxApiResponse<Void>> handleServiceException(ServiceException exception)
    {
        int code = exception.getCode() == null ? 40001 : exception.getCode();
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(WxApiResponse.failure(code, exception.getMessage()));
    }
}
