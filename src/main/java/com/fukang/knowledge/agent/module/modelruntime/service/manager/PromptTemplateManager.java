package com.fukang.knowledge.agent.module.modelruntime.service.manager;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class PromptTemplateManager {

    private final ConcurrentHashMap<String, PromptTemplate> cache = new ConcurrentHashMap<>();

    /**
     * 渲染 classpath:prompts 下的模板，并返回纯文本内容。
     */
    public String renderText(String templatePath, Map<String, Object> variables) {
        return getTemplate(templatePath).apply(safeVariables(variables)).text();
    }

    /**
     * 渲染模板为 SystemMessage，供 LangChain4j 对话模型直接使用。
     */
    public SystemMessage renderSystem(String templatePath, Map<String, Object> variables) {
        return getTemplate(templatePath).apply(safeVariables(variables)).toSystemMessage();
    }

    /**
     * 渲染模板为 UserMessage，避免调用方重复拼接 prompt 文本。
     */
    public UserMessage renderUser(String templatePath, Map<String, Object> variables) {
        return getTemplate(templatePath).apply(safeVariables(variables)).toUserMessage();
    }

    /**
     * 模板按路径缓存；模板文件随应用发布，不提供运行期刷新入口。
     */
    private PromptTemplate getTemplate(String templatePath) {
        return cache.computeIfAbsent(templatePath, key -> {
            String content = loadFromClasspath("prompts/" + key + ".md");
            log.info("Loaded prompt template: {}", key);
            return PromptTemplate.from(content);
        });
    }

    /**
     * LangChain4j 模板渲染不接受 null 变量表，这里统一兜底为空 Map。
     */
    private Map<String, Object> safeVariables(Map<String, Object> variables) {
        return variables != null ? variables : Collections.emptyMap();
    }

    /**
     * templatePath 形如 rag/answer-system.v1，对应 prompts/rag/answer-system.v1.md。
     */
    private String loadFromClasspath(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to load prompt template: " + path, e);
        }
    }
}
