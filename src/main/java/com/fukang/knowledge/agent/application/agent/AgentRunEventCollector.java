package com.fukang.knowledge.agent.application.agent;

import com.fukang.knowledge.agent.domain.agent.model.AgentRunEvent;

import java.util.ArrayList;
import java.util.List;

public class AgentRunEventCollector implements AutoCloseable {

    private static final ThreadLocal<AgentRunEventCollector> CURRENT = new ThreadLocal<>();

    private final List<AgentRunEvent> events = new ArrayList<>();

    public static AgentRunEventCollector open() {
        AgentRunEventCollector collector = new AgentRunEventCollector();
        CURRENT.set(collector);
        return collector;
    }

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

    @Override
    public void close() {
        CURRENT.remove();
    }
}
