package com.fukang.knowledge.agent.common.exception;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import lombok.Getter;

/**
 * 基础业务异常
 * <p>所有业务异常的统一基类，携带错误码和错误信息，
 * 由 {@link com.fukang.knowledge.agent.common.exception.GlobalExceptionHandler} 统一捕获处理</p>
 */
@Getter
public class BaseException extends RuntimeException {

    /** 业务错误码 */
    private final int code;

    /**
     * 通过错误码枚举构造业务异常
     *
     * @param errorCodeEnum 错误码枚举
     */
    public BaseException(ErrorCodeEnum errorCodeEnum) {
        super(errorCodeEnum.getMessage());
        this.code = errorCodeEnum.getCode();
    }

    /**
     * 通过自定义错误码和消息构造业务异常
     *
     * @param code    错误码
     * @param message 错误描述信息
     */
    public BaseException(int code, String message) {
        super(message);
        this.code = code;
    }
}
