package com.fukang.knowledge.agent.application.agent.tool;

import com.fukang.knowledge.agent.application.agent.port.ToolDefinitionRepository;
import com.fukang.knowledge.agent.common.enums.ExecutorTypeEnum;
import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;
import com.fukang.knowledge.agent.domain.agent.model.ToolInfo;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ToolDefinitionDO;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

/**
 * 工具注册表
 * <p>管理所有已注册的工具定义，使用 Caffeine 本地缓存提高访问性能。
 * 支持三种工具类型：HTTP、SQL、LOCAL_METHOD。
 * 缓存以工具名称为键，TTL 5 分钟，最大 100 个条目</p>
 */
@Slf4j
@Component
public class ToolRegistry implements ToolScope {

    private final ToolDefinitionRepository toolDefinitionRepository;
    private final Cache<String, ToolDefinition> toolCache;

    public ToolRegistry(ToolDefinitionRepository toolDefinitionRepository) {
        this.toolDefinitionRepository = toolDefinitionRepository;
        this.toolCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    /**
     * 获取所有可用工具的信息列表。
     */
    @Override
    public List<ToolInfo> listAvailableTools() {
        List<ToolDefinitionDO> dos = toolDefinitionRepository.findEnabled();
        return dos.stream()
                .map(this::toToolInfo)
                .toList();
    }

    /**
     * 按名称获取工具定义。
     */
    @Override
    public Optional<ToolDefinition> getTool(String name) {
        ToolDefinition cached = toolCache.getIfPresent(name);
        if (cached != null) {
            return Optional.of(cached);
        }
        ToolDefinitionDO toolDO = toolDefinitionRepository.findEnabledByName(name);
        if (toolDO == null) {
            return Optional.empty();
        }
        ToolDefinition definition = toDomain(toolDO);
        toolCache.put(name, definition);
        return Optional.of(definition);
    }

    private ToolInfo toToolInfo(ToolDefinitionDO toolDO) {
        return new ToolInfo(toolDO.getName(), toolDO.getDescription(), toolDO.getParametersSchema());
    }

    private ToolDefinition toDomain(ToolDefinitionDO toolDO) {
        return new ToolDefinition(
                toolDO.getId(),
                toolDO.getName(),
                toolDO.getDescription(),
                ExecutorTypeEnum.fromCode(toolDO.getExecutorType()),
                toolDO.getExecutorConfig(),
                toolDO.getParametersSchema(),
                toolDO.getEnabled()
        );
    }
}
