package com.fukang.knowledge.agent.application.agent.tool;

import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;
import com.fukang.knowledge.agent.domain.agent.model.ToolInfo;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 基于固定工具集合的运行时工具作用域。
 */
public class ScopedToolRegistry implements ToolScope {

    private final Map<String, ToolDefinition> toolsByName;

    /**
     * 根据业务方传入的工具集合创建只读作用域，过滤掉未启用工具。
     */
    public ScopedToolRegistry(List<ToolDefinition> tools) {
        Map<String, ToolDefinition> map = new LinkedHashMap<>();
        if (tools != null) {
            for (ToolDefinition tool : tools) {
                if (tool != null && Boolean.TRUE.equals(tool.enabled())) {
                    map.put(tool.name(), tool);
                }
            }
        }
        this.toolsByName = java.util.Collections.unmodifiableMap(map);
    }

    @Override
    public List<ToolInfo> listAvailableTools() {
        return toolsByName.values().stream()
                .map(tool -> new ToolInfo(tool.name(), tool.description(), tool.parametersSchema()))
                .toList();
    }

    @Override
    public Optional<ToolDefinition> getTool(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }
}
