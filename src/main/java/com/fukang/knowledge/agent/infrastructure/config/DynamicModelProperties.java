package com.fukang.knowledge.agent.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 动态模型配置属性
 * <p>控制模型实例本地缓存的过期时间和行为</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge-agent.model")
public class DynamicModelProperties {

    /** 模型实例本地缓存过期时间（秒），默认 300 秒（5 分钟） */
    private int cacheTtlSeconds = 300;

    /** 缓存最大容量，默认 50 个模型实例 */
    private int cacheMaxSize = 50;
}