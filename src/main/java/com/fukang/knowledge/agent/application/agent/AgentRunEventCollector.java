package com.fukang.knowledge.agent.application.agent;

import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;

import java.util.ArrayList;
import java.util.List;

/**
 * Agent 运行事件收集器。
 * <p>AiServices 的工具执行由 LangChain4j 回调触发，无法直接传入 runId。
 * 因此在单次执行范围内用 ThreadLocal 暂存事件，执行结束后统一写入 agent_run.log。</p>
 */
public class AgentRunEventCollector implements AutoCloseable {

    /** 当前线程正在执行的 Agent run 事件收集器。 */
    private static final ThreadLocal<AgentRunEventCollector> CURRENT = new ThreadLocal<>();

    private final List<AgentRunEvent> events = new ArrayList<>();

    /** 开启一次收集范围，通常放在 try-with-resources 中使用。 */
    public static AgentRunEventCollector open() {
        AgentRunEventCollector collector = new AgentRunEventCollector();
        CURRENT.set(collector);
        return collector;
    }

    /** 供工具执行回调记录事件；没有活动 run 时直接忽略。 */
    public static void record(AgentRunEvent event) {
        AgentRunEventCollector collector = CURRENT.get();
        if (collector != null) {
            collector.add(event);
        }
    }

    public void add(AgentRunEvent event) {
        events.add(event);
    }

    public List<AgentRunEvent> events() {
        return List.copyOf(events);
    }

    /** 关闭收集范围，防止线程复用时串入下一次 Agent run。 */
    @Override
    public void close() {
        CURRENT.remove();
    }
}
