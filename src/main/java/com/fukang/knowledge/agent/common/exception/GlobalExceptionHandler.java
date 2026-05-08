package com.fukang.knowledge.agent.common.exception;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.result.Result;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理器
 * <p>统一捕获并处理 Controller 层抛出的各类异常，将异常转换为标准的 {@link Result} 响应格式</p>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理业务异常
     *
     * @param e 业务异常
     * @return 包含错误码和错误信息的统一响应
     */
    @ExceptionHandler(BaseException.class)
    public Result<?> handleBaseException(BaseException e) {
        log.warn("业务异常: code={}, message={}", e.getCode(), e.getMessage());
        return Result.error(e.getCode(), e.getMessage());
    }

    /**
     * 处理参数校验异常（@Validated 触发）
     *
     * @param e 参数校验异常
     * @return 包含 400 错误码和校验失败信息的统一响应
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<?> handleValidationException(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getAllErrors().get(0).getDefaultMessage();
        log.warn("参数校验异常: {}", message);
        return Result.error(ErrorCodeEnum.BAD_REQUEST.getCode(), message);
    }

    /**
     * 处理系统未捕获的未知异常
     *
     * @param e 未知异常
     * @return 包含 500 错误码的统一响应
     */
    @ExceptionHandler(Exception.class)
    public Result<?> handleException(Exception e) {
        log.error("系统未捕获异常", e);
        return Result.error(ErrorCodeEnum.INTERNAL_SERVER_ERROR);
    }
}
