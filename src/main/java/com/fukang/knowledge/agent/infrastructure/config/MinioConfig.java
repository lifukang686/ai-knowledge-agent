package com.fukang.knowledge.agent.infrastructure.config;

import io.minio.MinioClient;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MinIO 对象存储配置
 * <p>通过 {@link ConfigurationProperties} 绑定 application.yml 中 minio 前缀的配置项，
 * 并创建 {@link MinioClient} Bean 供文件上传下载等业务使用</p>
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "minio")
public class MinioConfig {

    /** MinIO 服务端地址 */
    private String endpoint;
    /** 访问密钥 */
    private String accessKey;
    /** 密钥 */
    private String secretKey;
    /** 默认存储桶名称 */
    private String bucketName;

    /**
     * 创建 MinIO 客户端 Bean
     *
     * @return 配置好的 MinioClient 实例
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
    }
}
