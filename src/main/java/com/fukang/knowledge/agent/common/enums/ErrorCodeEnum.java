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
    AI_CALL_FAILED(2003, "AI 模型调用失败"),
    /** 无效的模型类型 */
    MODEL_TYPE_INVALID(2004, "无效的模型类型"),
    /** 已存在默认提供商，请先取消后再设置 */
    DEFAULT_PROVIDER_EXISTS(2005, "已存在默认提供商，请先取消后再设置"),
    /** 模型提供商BaseUrl不存在 */
    MODEL_BASE_URL_IS_NULL(2006, "模型提供商BaseURL不存在"),

    // ---- 知识库模块业务错误码 (3xxx) ----
    /** 上传文件不能为空 */
    FILE_EMPTY(3001, "上传文件不能为空"),
    /** 文件名不能为空 */
    FILE_NAME_EMPTY(3002, "文件名不能为空"),
    /** 不支持的文件类型 */
    FILE_TYPE_NOT_SUPPORTED(3003, "不支持的文件类型"),
    /** 文件上传失败 */
    FILE_UPLOAD_FAILED(3004, "文件上传失败"),
    /** 知识库不存在 */
    KNOWLEDGE_BASE_NOT_EXIST(3005, "知识库不存在"),
    /** 文档不存在 */
    DOCUMENT_NOT_EXIST(3006, "文档不存在"),
    /** 文件删除失败 */
    FILE_DELETE_FAILED(3007, "文件删除失败"),
    /** 文档解析失败 */
    DOCUMENT_PARSE_FAILED(3008, "文档解析失败"),
    /** 文档分块失败 */
    DOCUMENT_CHUNK_FAILED(3009, "文档分块失败"),
    /** 文档内容为空 */
    DOCUMENT_CONTENT_EMPTY(3010, "文档内容为空"),
    /** 文档块数据为空 */
    CHUNK_DATA_EMPTY(3011, "文档块数据为空"),
    /** 文档块存储失败 */
    CHUNK_STORAGE_FAILED(3012, "文档块存储失败"),
    /** 文档块数据校验失败 */
    CHUNK_VALIDATION_FAILED(3013, "文档块数据校验失败"),
    /** 未找到可用的嵌入模型 */
    NO_EMBEDDING_MODEL_AVAILABLE(3014, "未找到可用的嵌入模型"),
    /** 向量嵌入计算失败 */
    EMBEDDING_FAILED(3015, "向量嵌入计算失败"),
    /** 向量存储失败 */
    VECTOR_STORAGE_FAILED(3016, "向量存储失败"),

    // ---- RAG 模块业务错误码 (35xx) ----
    /** 问题不能为空 */
    QUESTION_EMPTY(3501, "问题不能为空"),
    /** 查询改写失败 */
    QUERY_REWRITE_FAILED(3502, "查询改写失败"),

    // ---- 动态模型模块业务错误码 (4xxx) ----
    /** 未找到可用的模型提供商 */
    NO_MODEL_PROVIDER_AVAILABLE(4001, "未找到可用的模型提供商"),
    /** 模型工厂创建模型实例失败 */
    MODEL_CREATION_FAILED(4002, "创建模型实例失败"),
    /** 未找到该类型的模型配置 */
    NO_MODEL_CONFIG_AVAILABLE(4003, "未找到该类型的模型配置");

    /** 错误码 */
    private final int code;
    /** 错误描述信息 */
    private final String message;
}
