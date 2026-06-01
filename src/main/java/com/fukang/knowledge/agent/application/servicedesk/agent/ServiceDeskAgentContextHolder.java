package com.fukang.knowledge.agent.application.servicedesk.agent;

/**
 * 服务台 Agent 当前运行上下文持有器。
 */
public final class ServiceDeskAgentContextHolder {

    private static final ThreadLocal<ServiceDeskAgentContext> CURRENT = new ThreadLocal<>();

    private ServiceDeskAgentContextHolder() {
    }

    public static void set(ServiceDeskAgentContext context) {
        CURRENT.set(context);
    }

    public static ServiceDeskAgentContext getRequired() {
        ServiceDeskAgentContext context = CURRENT.get();
        if (context == null) {
            throw new IllegalStateException("服务台 Agent 上下文不存在");
        }
        return context;
    }

    public static void clear() {
        CURRENT.remove();
    }
}
