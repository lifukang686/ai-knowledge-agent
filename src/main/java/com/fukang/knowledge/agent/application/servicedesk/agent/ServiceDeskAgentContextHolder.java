package com.fukang.knowledge.agent.application.servicedesk.agent;

/**
 * 服务台 Agent 当前运行上下文持有器。
 */
public final class ServiceDeskAgentContextHolder {

    /**
     * 当前线程内的服务台 Agent 上下文。
     */
    private static final ThreadLocal<ServiceDeskAgentContext> CURRENT = new ThreadLocal<>();

    private ServiceDeskAgentContextHolder() {
    }

    /**
     * 设置当前上下文。
     */
    public static void set(ServiceDeskAgentContext context) {
        CURRENT.set(context);
    }

    /**
     * 获取当前上下文，不存在时直接失败。
     */
    public static ServiceDeskAgentContext getRequired() {
        ServiceDeskAgentContext context = CURRENT.get();
        if (context == null) {
            throw new IllegalStateException("服务台 Agent 上下文不存在");
        }
        return context;
    }

    /**
     * 清理当前上下文。
     */
    public static void clear() {
        CURRENT.remove();
    }
}
