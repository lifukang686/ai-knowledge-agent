package com.fukang.knowledge.agent.infrastructure.config;

import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Jackson 序列化配置
 * <p>将装箱 Long 类型全局序列化为字符串，避免前端 JavaScript 大整数精度丢失。
 * 原始 long 类型（如 documentCount、page、pageSize、total 等计数/分页字段）
 * 保持数字类型不变，确保前端可直接用于数值运算。</p>
 */
@Configuration
public class JacksonConfig {

    @Bean
    public Jackson2ObjectMapperBuilderCustomizer longToStringCustomizer() {
        return builder -> {
            builder.serializerByType(Long.class, ToStringSerializer.instance);
        };
    }
}