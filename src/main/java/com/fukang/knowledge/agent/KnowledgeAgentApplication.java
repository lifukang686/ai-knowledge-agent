package com.fukang.knowledge.agent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 企业知识库 AI Agent 平台启动类
 * <p>基于 Spring Boot 3.x 构建，集成 Spring AI、MyBatis-Plus、MinIO、Redis 等中间件，
 * 提供用户认证、AI 模型管理和调用等核心能力</p>
 */
@SpringBootApplication
public class KnowledgeAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(KnowledgeAgentApplication.class, args);
    }
}
