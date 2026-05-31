package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.application.agent.AgentAiService;
import com.fukang.knowledge.agent.application.agent.AgentRunEventCollector;
import com.fukang.knowledge.agent.application.agent.port.AiServicesAgentRuntime;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;
import com.fukang.knowledge.agent.domain.agent.model.ExecutionStrategy;
import com.fukang.knowledge.agent.infrastructure.tool.DynamicToolProvider;
import dev.langchain4j.service.AiServices;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * LangChain4j AiServices 运行时适配器。
 * <p>封装具体框架构建细节，应用层只关心输入任务、系统提示词和结构化事件。</p>
 */
@Component
@RequiredArgsConstructor
public class LangChain4jAiServicesAgentRuntime implements AiServicesAgentRuntime {

    private final DynamicModelManager dynamicModelManager;
    private final AgentMemoryFactory memoryFactory;
    private final DynamicToolProvider dynamicToolProvider;

    @Override
    public ExecutionResult execute(String task, String systemPrompt) {
        try (AgentRunEventCollector collector = AgentRunEventCollector.open()) {
            collector.add(AgentRunEvent.of(AgentRunEvent.EventType.REASONING, null, null,
                    Map.of("strategy", ExecutionStrategy.AI_SERVICES.name()), true, null,
                    "AiServices execution started"));

            AgentAiService aiService = AiServices
                    .builder(AgentAiService.class)
                    .chatLanguageModel(dynamicModelManager.getChatModel(ModelTypeEnum.CHAT))
                    .systemMessageProvider(memoryId -> systemPrompt)
                    .toolProvider(dynamicToolProvider)
                    .chatMemory(memoryFactory.createDefault())
                    .build();

            return new ExecutionResult(aiService.chat(task), collector.events());
        }
    }
}
