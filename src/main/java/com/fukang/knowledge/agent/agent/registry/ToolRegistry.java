package com.fukang.knowledge.agent.agent.registry;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.api.agent.dto.ToolResp;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ExecutorTypeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;
import com.fukang.knowledge.agent.domain.agent.model.ToolInfo;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ToolDefinitionDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ToolDefinitionMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * 工具注册表
 * <p>管理所有已注册的工具定义，使用 Caffeine 本地缓存提高访问性能。
 * 支持三种工具类型：HTTP、SQL、LOCAL_METHOD。
 * 缓存以工具名称为键，TTL 5 分钟，最大 100 个条目</p>
 */
@Slf4j
@Component
public class ToolRegistry {

    private final ToolDefinitionMapper toolDefinitionMapper;
    private final Cache<String, ToolDefinition> toolCache;

    public ToolRegistry(ToolDefinitionMapper toolDefinitionMapper) {
        this.toolDefinitionMapper = toolDefinitionMapper;
        this.toolCache = Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(100)
                .build();
    }

    /**
     * 获取所有可用工具的信息列表（供 Planner 使用）
     * <p>每次从数据库查询启用的工具，确保工具列表实时准确</p>
     *
     * @return 启用工具的信息列表
     */
    public List<ToolInfo> listAvailableTools() {
        List<ToolDefinitionDO> dos = toolDefinitionMapper.selectList(
                new LambdaQueryWrapper<ToolDefinitionDO>()
                        .eq(ToolDefinitionDO::getEnabled, true)
        );
        return dos.stream()
                .map(this::toToolInfo)
                .toList();
    }

    /**
     * 分页查询工具列表
     * <p>支持按关键字模糊搜索名称和描述，返回含完整信息的工具分页数据</p>
     *
     * @param pageQuery 分页参数对象，包含 current(页码) 和 size(每页条数)
     * @param keyword   搜索关键字，可选，模糊匹配名称和描述
     * @return 分页响应，包含工具信息列表
     */
    public PageResponse<ToolResp> listTools(Page<?> pageQuery, String keyword) {
        LambdaQueryWrapper<ToolDefinitionDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(ToolDefinitionDO::getName, keyword)
                    .or()
                    .like(ToolDefinitionDO::getDescription, keyword));
        }
        wrapper.orderByDesc(ToolDefinitionDO::getCreateTime);

        IPage<ToolDefinitionDO> resultPage = toolDefinitionMapper.selectPage(
                new Page<>(pageQuery.getCurrent(), pageQuery.getSize()), wrapper);

        List<ToolResp> items = resultPage.getRecords().stream()
                .map(this::toToolResp)
                .collect(Collectors.toList());

        return new PageResponse<>(items, resultPage.getTotal(),
                resultPage.getCurrent(), resultPage.getSize());
    }

    /**
     * 按名称获取工具定义
     * <p>先从 Caffeine 缓存获取，未命中则查数据库并写入缓存</p>
     *
     * @param name 工具名称
     * @return 工具定义（不存在返回空 Optional）
     */
    public Optional<ToolDefinition> getTool(String name) {
        ToolDefinition cached = toolCache.getIfPresent(name);
        if (cached != null) {
            return Optional.of(cached);
        }
        ToolDefinitionDO toolDO = toolDefinitionMapper.selectOne(
                new LambdaQueryWrapper<ToolDefinitionDO>()
                        .eq(ToolDefinitionDO::getName, name)
                        .eq(ToolDefinitionDO::getEnabled, true)
        );
        if (toolDO == null) {
            return Optional.empty();
        }
        ToolDefinition definition = toDomain(toolDO);
        toolCache.put(name, definition);
        return Optional.of(definition);
    }

    /**
     * 注册工具定义
     * <p>向数据库插入工具定义记录并清除对应名称的缓存</p>
     *
     * @param toolDefinition 工具定义
     */
    public void register(ToolDefinition toolDefinition) {
        ToolDefinitionDO toolDO = toDO(toolDefinition);
        toolDefinitionMapper.insert(toolDO);
        toolCache.invalidate(toolDefinition.name());
        log.info("工具注册成功: name={}, type={}", toolDefinition.name(), toolDefinition.executorType());
    }

    /**
     * 更新工具定义
     * <p>先查询旧记录以获取旧名称用于缓存失效，再执行数据库更新</p>
     *
     * @param id             工具 ID
     * @param toolDefinition 工具定义更新信息
     * @return 更新后的工具响应对象
     */
    public ToolResp update(Long id, ToolDefinition toolDefinition) {
        ToolDefinitionDO oldDO = toolDefinitionMapper.selectById(id);
        if (oldDO == null) {
            throw new BaseException(
                    ErrorCodeEnum.NOT_FOUND.getCode(),
                    "工具不存在");
        }

        String oldName = oldDO.getName();
        ToolDefinitionDO updatedDO = toDO(toolDefinition);
        updatedDO.setId(id);
        toolDefinitionMapper.updateById(updatedDO);

        toolCache.invalidate(oldName);
        if (!oldName.equals(toolDefinition.name())) {
            toolCache.invalidate(toolDefinition.name());
        }

        log.info("工具更新成功: id={}, oldName={}, newName={}", id, oldName, toolDefinition.name());
        return new ToolResp(id, toolDefinition.name(), toolDefinition.description(),
                toolDefinition.executorType(), toolDefinition.executorConfig(),
                toolDefinition.parametersSchema(), toolDefinition.enabled());
    }

    /**
     * 按 ID 删除工具定义
     * <p>先查询确认工具存在并获取名称用于缓存失效，再执行数据库删除</p>
     *
     * @param id 工具 ID
     */
    public void deleteById(Long id) {
        ToolDefinitionDO toolDO = toolDefinitionMapper.selectById(id);
        if (toolDO == null) {
            throw new BaseException(
                    ErrorCodeEnum.NOT_FOUND.getCode(),
                    "工具不存在");
        }
        toolDefinitionMapper.deleteById(id);
        toolCache.invalidate(toolDO.getName());
        log.info("工具已删除: id={}, name={}", id, toolDO.getName());
    }

    /**
     * 按 ID 列表批量获取工具定义
     * <p>用于根据 Agent 配置的 toolIds 加载关联工具</p>
     *
     * @param ids 工具 ID 列表
     * @return 工具定义列表
     */
    public List<ToolDefinition> getToolsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<ToolDefinitionDO> toolDOs = toolDefinitionMapper.selectBatchIds(ids);
        return toolDOs.stream()
                .map(this::toDomain)
                .toList();
    }

    /**
     * 删除工具定义
     * <p>从数据库删除并清除缓存</p>
     *
     * @param name 工具名称
     */
    public void unregister(String name) {
        toolDefinitionMapper.delete(
                new LambdaQueryWrapper<ToolDefinitionDO>()
                        .eq(ToolDefinitionDO::getName, name)
        );
        toolCache.invalidate(name);
        log.info("工具已注销: name={}", name);
    }

    /**
     * 清除所有缓存
     */
    public void evictCache() {
        toolCache.invalidateAll();
        log.info("工具注册表缓存已全部清除");
    }

    private ToolInfo toToolInfo(ToolDefinitionDO toolDO) {
        return new ToolInfo(toolDO.getName(), toolDO.getDescription(), toolDO.getParametersSchema());
    }

    private ToolResp toToolResp(ToolDefinitionDO toolDO) {
        return new ToolResp(
                toolDO.getId(),
                toolDO.getName(),
                toolDO.getDescription(),
                ExecutorTypeEnum.fromCode(toolDO.getExecutorType()),
                toolDO.getExecutorConfig(),
                toolDO.getParametersSchema(),
                toolDO.getEnabled()
        );
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

    private ToolDefinitionDO toDO(ToolDefinition toolDefinition) {
        ToolDefinitionDO toolDO = new ToolDefinitionDO();
        toolDO.setName(toolDefinition.name());
        toolDO.setDescription(toolDefinition.description());
        toolDO.setExecutorType(toolDefinition.executorType().getCode());
        toolDO.setExecutorConfig(toolDefinition.executorConfig());
        toolDO.setParametersSchema(toolDefinition.parametersSchema());
        toolDO.setEnabled(toolDefinition.enabled() != null ? toolDefinition.enabled() : true);
        return toolDO;
    }
}