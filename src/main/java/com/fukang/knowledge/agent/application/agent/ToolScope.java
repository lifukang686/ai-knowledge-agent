package com.fukang.knowledge.agent.application.agent;

import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;
import com.fukang.knowledge.agent.domain.agent.model.ToolInfo;

import java.util.List;
import java.util.Optional;

/**
 * Agent 单次运行可见的工具作用域。
 * <p>通用 Agent 可使用全局启用工具；业务 Agent 可传入受限作用域，避免 LLM 规划到越权工具。</p>
 */
public interface ToolScope {

    /** 返回给 Planner 的工具摘要列表。 */
    List<ToolInfo> listAvailableTools();

    /** 按名称获取运行时工具定义。 */
    Optional<ToolDefinition> getTool(String name);
}
