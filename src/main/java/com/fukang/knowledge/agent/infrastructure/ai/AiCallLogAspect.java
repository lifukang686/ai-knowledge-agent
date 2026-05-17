package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.AiCallLogDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.AiCallLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.ai.chat.metadata.Usage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * AI 调用日志 AOP 切面（动态模型版本）
 * <p>拦截 SpringAiClientImpl.call() 方法，记录每次 AI 调用的详细日志，
 * 包括模型名称、提示词、响应内容、Token 消耗和耗时</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AiCallLogAspect {

    private static final int TOKEN_WARN_THRESHOLD = 4000;
    private final AiCallLogMapper aiCallLogMapper;
    private final DynamicModelManager modelManager;

    @Around("execution(* com.fukang.knowledge.agent.infrastructure.ai.SpringAiClientImpl.call(..))")
    public Object logAiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        Object[] args = joinPoint.getArgs();
        String prompt = args.length > 0 ? (String) args[0] : "";
        String modelName;
        try {
            modelName = modelManager.resolveModelName(ModelTypeEnum.CHAT);
        } catch (Exception e) {
            modelName = "unknown";
        }

        long start = System.currentTimeMillis();
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long latency = System.currentTimeMillis() - start;
            int tokenUsage = 0;
            String responseText = "";

            if (result instanceof ChatResponse chatResponse) {
                responseText = chatResponse.getResult().getOutput().getText();
                Usage usage = chatResponse.getMetadata().getUsage();
                if (usage != null) {
                    tokenUsage = usage.getTotalTokens().intValue();
                    if (tokenUsage > TOKEN_WARN_THRESHOLD) {
                        log.warn("Token 消耗预警: 达到了 {} tokens", tokenUsage);
                    }
                }
            }
            final String finalModelName = modelName;
            final String finalPrompt = prompt;
            final String finalResponseText = responseText;
            final int finalTokenUsage = tokenUsage;
            saveLogAsync(finalModelName, finalPrompt, finalResponseText, finalTokenUsage, (int) latency);
        }
    }

    private void saveLogAsync(String modelName, String prompt, String response, int tokenUsage, int latency) {
        CompletableFuture.runAsync(() -> {
            try {
                AiCallLogDO logDo = new AiCallLogDO();
                logDo.setModelId(1L);
                logDo.setPrompt(modelName + " | " + prompt);
                logDo.setResponse(response);
                logDo.setTokenUsage(tokenUsage);
                logDo.setLatencyMs(latency);
                aiCallLogMapper.insert(logDo);
            } catch (Exception e) {
                log.error("保存 AI 调用日志失败", e);
            }
        });
    }
}