package com.fukang.knowledge.agent.application.model;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.infrastructure.ai.SpringAiClientImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Service;

/**
 * AI 调用服务（动态模型版本）
 * <p>封装统一的 AI 模型调用接口，通过 SpringAiClientImpl 动态选择模型，
 * 处理模型调用、Token 消耗统计与预警</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallService {

    private static final long TOKEN_WARN_THRESHOLD = 4000;
    private final SpringAiClientImpl springAiClient;

    /**
     * 调用 AI 聊天模型
     * <p>使用动态 CHAT 模型进行对话，记录调用耗时和 Token 消耗</p>
     *
     * @param prompt 提示词
     * @return AI 模型返回的文本内容
     */
    public String callModel(String prompt) {
        long start = System.currentTimeMillis();
        ChatResponse response = springAiClient.call(prompt);
        long latency = System.currentTimeMillis() - start;
        String content = response.getResult().getOutput().getText();

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