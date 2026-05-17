package com.fukang.knowledge.agent.infrastructure.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 文档处理线程池配置属性
 * <p>使用 @ConfigurationProperties 进行类型安全的配置绑定，
 * 所有参数均提供合理默认值，可通过 application.yml 覆写</p>
 */
@Data
@Component
@ConfigurationProperties(prefix = "knowledge-agent.document-processing")
public class DocumentProcessingProperties {

    /** 核心线程数，默认 2 */
    private int corePoolSize = 2;

    /** 最大线程数，默认 4 */
    private int maxPoolSize = 4;

    /** 队列容量，默认 100 */
    private int queueCapacity = 100;

    /** 嵌入阶段最大重试次数，默认 3 */
    private int embedRetryMax = 3;

    /** 嵌入阶段重试基础退避秒数，默认 2 */
    private int embedRetryBaseSeconds = 2;
}