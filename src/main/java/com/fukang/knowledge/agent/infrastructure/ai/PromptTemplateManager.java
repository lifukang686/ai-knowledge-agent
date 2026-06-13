package com.fukang.knowledge.agent.infrastructure.ai;

import dev.langchain4j.data.message.SystemMessage;
import dev.langchain4j.data.message.UserMessage;
import dev.langchain4j.model.input.PromptTemplate;
import lombok.extern.slf4j.Slf4j;
import org.intellij.lang.annotations.Language;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Prompt 模板管理器
 * <p>统一管理所有 LLM 提示词模板，从 classpath 下的 {@code prompts/} 目录加载。
 * 使用 LangChain4j 的 {@link PromptTemplate}（Mustache 语法），
 * 替代散落在 Java 类中的 {@code String.replace()} 硬编码占位符。
 *
 * <pre>
 * 使用示例：
 * {@code
 * // 返回渲染后的纯文本
 * String text = promptTemplateManager.renderText("agent/planning",
 *         Map.of("tools", toolsDesc, "task", task));
 *
 * // 直接获取 SystemMessage / UserMessage
 * SystemMessage msg = promptTemplateManager.renderSystem("agent/planning", vars);
 * UserMessage msg = promptTemplateManager.renderUser("agent/reasoning", vars);
 * }
 * </pre>
 * </p>
 *
 * <p>模板文件位于 {@code src/main/resources/prompts/}，以 {@code .md} 为扩展名。
 * 支持运行时刷新缓存（配合文件监听可实现热更新）</p>
 */
@Slf4j
@Component
public class PromptTemplateManager {

    private final ConcurrentHashMap<String, PromptTemplate> cache = new ConcurrentHashMap<>();

    /**
     * 渲染模板并返回纯文本
     *
     * @param templatePath 模板路径，如 "agent/planning"（对应 prompts/agent/planning.md）
     * @param variables    模板变量
     * @return 渲染后的字符串
     */
    public String renderText(String templatePath, Map<String, Object> variables) {
        PromptTemplate template = getTemplate(templatePath);
        return template.apply(variables != null ? variables : Collections.emptyMap()).text();
    }

    /**
     * 渲染模板并返回 SystemMessage
     *
     * @param templatePath 模板路径
     * @param variables    模板变量
     * @return SystemMessage
     */
    public SystemMessage renderSystem(String templatePath, Map<String, Object> variables) {
        PromptTemplate template = getTemplate(templatePath);
        return template.apply(variables != null ? variables : Collections.emptyMap()).toSystemMessage();
    }

    /**
     * 渲染模板并返回 UserMessage
     *
     * @param templatePath 模板路径
     * @param variables    模板变量
     * @return UserMessage
     */
    public UserMessage renderUser(String templatePath, Map<String, Object> variables) {
        PromptTemplate template = getTemplate(templatePath);
        return template.apply(variables != null ? variables : Collections.emptyMap()).toUserMessage();
    }

    /**
     * 获取 PromptTemplate（带缓存）
     *
     * @param templatePath 模板路径，如 "agent/planning"
     * @return PromptTemplate 实例
     */
    public PromptTemplate getTemplate(String templatePath) {
        return cache.computeIfAbsent(templatePath, key -> {
            String content = loadFromClasspath("prompts/" + key + ".md");
            log.info("加载 Prompt 模板: {}", key);
            return PromptTemplate.from(content);
        });
    }

    /**
     * 刷新缓存（用于配置变更后重新加载）
     *
     * @param templatePath 模板路径
     */
    public void refresh(String templatePath) {
        cache.remove(templatePath);
        log.info("Prompt 模板缓存已刷新: {}", templatePath);
    }

    /**
     * 从 classpath 加载模板文件
     */
    private String loadFromClasspath(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalArgumentException("Prompt 模板加载失败: " + path, e);
        }
    }
}
