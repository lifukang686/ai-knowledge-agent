package com.fukang.knowledge.agent.infrastructure.ai;

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
 * AI 调用日志切面
 * <p>通过 AOP 拦截 {@link SpringAiClientImpl#call} 方法，自动记录每次 AI 调用的
 * 提示词、响应内容、Token 消耗和耗时，并异步持久化到数据库</p>
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AiCallLogAspect {

    /** Token 消耗预警阈值 */
    private static final int TOKEN_WARN_THRESHOLD = 4000;

    private final AiCallLogMapper aiCallLogMapper;

    /**
     * 环绕通知：拦截 AI 调用方法，记录调用日志
     * <p>在方法执行前后记录耗时，从响应中提取 Token 消耗和文本内容，
     * 超过阈值时发出预警，最终异步保存调用日志到数据库</p>
     *
     * @param joinPoint 切面连接点
     * @return AI 调用的原始返回值
     * @throws Throwable 方法执行过程中可能抛出的异常
     */
    @Around("execution(* com.fukang.knowledge.agent.infrastructure.ai.SpringAiClientImpl.call(..))")
    public Object logAiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        // 提取方法参数
        Object[] args = joinPoint.getArgs();
        String prompt = args.length > 0 ? (String) args[0] : "";
        String modelName = args.length > 1 ? (String) args[1] : "unknown";

        long start = System.currentTimeMillis();
        Object result = null;
        try {
            result = joinPoint.proceed();
            return result;
        } finally {
            long latency = System.currentTimeMillis() - start;
            int tokenUsage = 0;
            String responseText = "";

            // 从 ChatResponse 中提取 Token 消耗和响应文本
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

            // 异步保存调用日志，不阻塞主流程
            saveLogAsync(1L, prompt, responseText, tokenUsage, (int) latency);
        }
    }

    /**
     * 异步保存 AI 调用日志到数据库
     *
     * @param modelId    模型配置ID
     * @param prompt     用户输入的提示词
     * @param response   AI 返回的响应文本
     * @param tokenUsage 本次调用消耗的 Token 数
     * @param latency    本次调用耗时（毫秒）
     */
    private void saveLogAsync(Long modelId, String prompt, String response, int tokenUsage, int latency) {
        CompletableFuture.runAsync(() -> {
            try {
                AiCallLogDO logDo = new AiCallLogDO();
                logDo.setModelId(modelId);
                logDo.setPrompt(prompt);
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
