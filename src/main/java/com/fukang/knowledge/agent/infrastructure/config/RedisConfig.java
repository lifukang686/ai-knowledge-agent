package com.fukang.knowledge.agent.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;

/**
 * Redis 配置
 * <p>自定义 RedisTemplate 的序列化策略：Key 使用 String 序列化，Value 使用 JSON 序列化，
 * 避免默认 JDK 序列化导致的可读性差和跨语言兼容问题</p>
 */
@Configuration
public class RedisConfig {

    /**
     * 创建自定义 RedisTemplate Bean
     * <p>Key 和 HashKey 使用 StringRedisSerializer，Value 和 HashValue 使用
     * GenericJackson2JsonRedisSerializer，确保 Redis 中存储的数据可读且支持 JSON 反序列化</p>
     *
     * @param connectionFactory Redis 连接工厂，由 Spring Boot 自动配置
     * @return 配置好的 RedisTemplate 实例
     */
    @Bean
    public RedisTemplate<String, Object> redisTemplate(RedisConnectionFactory connectionFactory) {
        RedisTemplate<String, Object> template = new RedisTemplate<>();
        template.setConnectionFactory(connectionFactory);

        // Key 序列化器：使用 String 格式，便于在 Redis 客户端中查看
        StringRedisSerializer stringRedisSerializer = new StringRedisSerializer();
        template.setKeySerializer(stringRedisSerializer);
        template.setHashKeySerializer(stringRedisSerializer);

        // Value 序列化器：使用 JSON 格式，支持复杂对象的序列化与反序列化
        GenericJackson2JsonRedisSerializer jsonRedisSerializer = new GenericJackson2JsonRedisSerializer();
        template.setValueSerializer(jsonRedisSerializer);
        template.setHashValueSerializer(jsonRedisSerializer);

        template.afterPropertiesSet();
        return template;
    }
}
