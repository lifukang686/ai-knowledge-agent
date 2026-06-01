package com.fukang.knowledge.agent.infrastructure.tool;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 本地方法工具注册表。
 * <p>只注册实现了 {@link LocalMethodTool} 的 Spring Bean，避免任意反射调用带来的安全风险。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LocalMethodToolRegistry {

    private final List<LocalMethodTool> tools;
    private final Map<String, LocalMethodTool> toolsByName = new LinkedHashMap<>();

    @PostConstruct
    void init() {
        for (LocalMethodTool tool : tools) {
            toolsByName.put(tool.name(), tool);
            log.info("注册本地方法工具: {}", tool.name());
        }
    }

    public Optional<LocalMethodTool> get(String name) {
        return Optional.ofNullable(toolsByName.get(name));
    }
}
