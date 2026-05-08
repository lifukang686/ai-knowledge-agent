package com.fukang.knowledge.agent.common.result;

import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import lombok.Data;

/**
 * 统一响应封装
 * <p>所有 API 接口的返回值统一使用此结构，包含状态码、消息、数据和时间戳</p>
 *
 * @param <T> 响应数据的类型
 */
@Data
public class Result<T> {

    /** 状态码 */
    private int code;
    /** 响应消息 */
    private String message;
    /** 响应数据 */
    private T data;
    /** 响应时间戳（毫秒） */
    private long timestamp;

    private Result() {
        this.timestamp = System.currentTimeMillis();
    }

    /**
     * 构建成功响应（带数据）
     *
     * @param data 响应数据
     * @param <T>  数据类型
     * @return 成功的统一响应
     */
    public static <T> Result<T> success(T data) {
        Result<T> result = new Result<>();
        result.setCode(ErrorCodeEnum.SUCCESS.getCode());
        result.setMessage(ErrorCodeEnum.SUCCESS.getMessage());
        result.setData(data);
        return result;
    }

    /**
     * 构建成功响应（无数据）
     *
     * @param <T> 数据类型
     * @return 成功的统一响应
     */
    public static <T> Result<T> success() {
        return success(null);
    }

    /**
     * 通过错误码枚举构建失败响应
     *
     * @param errorCode 错误码枚举
     * @param <T>       数据类型
     * @return 失败的统一响应
     */
    public static <T> Result<T> error(ErrorCodeEnum errorCode) {
        Result<T> result = new Result<>();
        result.setCode(errorCode.getCode());
        result.setMessage(errorCode.getMessage());
        return result;
    }

    /**
     * 通过自定义错误码和消息构建失败响应
     *
     * @param code    错误码
     * @param message 错误描述信息
     * @param <T>     数据类型
     * @return 失败的统一响应
     */
    public static <T> Result<T> error(int code, String message) {
        Result<T> result = new Result<>();
        result.setCode(code);
        result.setMessage(message);
        return result;
    }
}
