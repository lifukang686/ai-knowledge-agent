package com.fukang.knowledge.agent.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 工具执行器类型枚举
 * <p>定义 Agent 工具的执行方式分类</p>
 */
@Getter
@AllArgsConstructor
public enum ExecutorTypeEnum {

    HTTP("HTTP", "HTTP 接口调用"),

    SQL("SQL", "SQL 数据库查询"),

    /**
     * 调用受控 Spring Bean 工具。
     * <p>仅允许执行实现 LocalMethodTool 的已注册 Bean，不支持任意反射。</p>
     */
    LOCAL_METHOD("LOCAL_METHOD", "本地方法调用");

    private final String code;

    private final String description;

    public static ExecutorTypeEnum fromCode(String code) {
        for (ExecutorTypeEnum type : values()) {
            if (type.code.equalsIgnoreCase(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("不支持的工具执行器类型: " + code);
    }
}
