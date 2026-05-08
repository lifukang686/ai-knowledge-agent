package com.fukang.knowledge.agent.application.model;

import com.fukang.knowledge.agent.infrastructure.ai.SpringAiClientImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

/**
 * AI 调用服务
 * <p>封装统一的 AI 模型调用接口，处理模型调用、Token 消耗统计与预警</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallService {

    /** Token 消耗预警阈值 */
    private static final long TOKEN_WARN_THRESHOLD = 4000;

    private final SpringAiClientImpl springAiClient;

    /**
     * 调用 AI 模型并返回文本结果
     * <p>根据 modelId 查询对应的模型配置，调用 Spring AI 客户端发起请求，
     * 并记录调用耗时和 Token 消耗，超过阈值时发出预警</p>
     *
     * @param prompt  用户输入的提示词
     * @param modelId 模型配置ID（当前简化为直接使用默认模型）
     * @return AI 模型返回的文本内容
     */
    public String callModel(String prompt, Long modelId) {
        long start = System.currentTimeMillis();

        // 根据 modelId 查询具体提供商与模型名（当前简化为直接调用默认模型）
        ChatResponse response = springAiClient.call(prompt, "gpt-3.5-turbo");

        long latency = System.currentTimeMillis() - start;

        // 提取 AI 返回的文本内容
        String content = response.getResult().getOutput().getText();

        // 处理 Token 消耗统计与预警
        Usage usage = response.getMetadata().getUsage();
        if (usage != null) {
            long totalTokens = usage.getTotalTokens();
            log.info("AI 调用完成, 耗时: {}ms, 消耗 Token: {}", latency, totalTokens);

            if (totalTokens > TOKEN_WARN_THRESHOLD) {
                log.warn("Token 消耗预警: 本次消耗达到了 {} tokens", totalTokens);
            }
        }

        return content;
    }
}
