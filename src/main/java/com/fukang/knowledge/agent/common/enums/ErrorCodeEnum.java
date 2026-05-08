package com.fukang.knowledge.agent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 全局错误码枚举
 * <p>定义系统所有业务错误码，HTTP 标准错误码使用 4xx/5xx，业务错误码从 1000 起编号</p>
 */
@Getter
@AllArgsConstructor
public enum ErrorCodeEnum {

    // ---- HTTP 标准错误码 ----
    SUCCESS(200, "操作成功"),
    BAD_REQUEST(400, "参数错误"),
    UNAUTHORIZED(401, "未授权，请登录"),
    FORBIDDEN(403, "拒绝访问"),
    NOT_FOUND(404, "资源不存在"),
    INTERNAL_SERVER_ERROR(500, "系统内部错误"),

    // ---- 认证模块业务错误码 (1xxx) ----
    /** 用户不存在 */
    USER_NOT_EXIST(1001, "用户不存在"),
    /** 密码错误 */
    PASSWORD_ERROR(1002, "密码错误"),
    /** Token 无效或已过期 */
    TOKEN_INVALID(1003, "Token无效或已过期"),

    // ---- 模型模块业务错误码 (2xxx) ----
    /** 模型提供商不存在 */
    PROVIDER_NOT_EXIST(2001, "模型提供商不存在"),
    /** 模型配置不存在 */
    MODEL_NOT_EXIST(2002, "模型配置不存在"),
    /** AI 模型调用失败 */
    AI_CALL_FAILED(2003, "AI 模型调用失败");

    /** 错误码 */
    private final int code;
    /** 错误描述信息 */
    private final String message;
}
