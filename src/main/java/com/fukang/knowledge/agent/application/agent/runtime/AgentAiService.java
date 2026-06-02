package com.fukang.knowledge.agent.application.agent.runtime;

/**
 * Agent AI Service 接口
 * <p>定义 AiServices 代理的方法签名。由 LangChain4j {@code AiServices.builder()}
 * 动态生成实现类，内置 ReAct（Thought → Action → Observation）工具调用循环。
 *
 * <pre>
 * 使用方式：
 * {@code
 * AgentAiService aiService = AiServices.builder(AgentAiService.class)
 *         .chatLanguageModel(chatModel)
 *         .systemMessageProvider(memoryId -> agentSystemPrompt)
 *         .toolProvider(dynamicToolProvider)
 *         .build();
 * String answer = aiService.chat("查询华东区销售数据");
 * }
 * </pre>
 * </p>
 */
public interface AgentAiService {

    /**
     * 执行用户任务（ReAct 模式：LLM 自主决策何时调用哪个工具）
     *
     * @param userMessage 用户输入任务
     * @return Agent 执行结果
     */
    String chat(String userMessage);
}
