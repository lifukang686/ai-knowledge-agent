package com.fukang.knowledge.agent.api.agent;

import com.fukang.knowledge.agent.api.agent.dto.AgentCreateReq;
import com.fukang.knowledge.agent.api.agent.dto.AgentResp;
import com.fukang.knowledge.agent.api.agent.dto.AgentRunReq;
import com.fukang.knowledge.agent.application.agent.AgentRunResult;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.Result;
import com.fukang.knowledge.agent.application.agent.AgentAppService;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.AgentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.AgentMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Agent 控制器
 * <p>提供 Agent 配置管理和任务执行接口</p>
 */
@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentAppService agentAppService;
    private final AgentMapper agentMapper;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 执行 Agent 任务
     *
     * @param agentId Agent 配置 ID
     * @param req     任务请求（含 task 描述）
     * @return Agent 运行结果
     */
    @PostMapping("/{agentId}/run")
    public Result<AgentRunResult> run(@PathVariable Long agentId, @RequestBody AgentRunReq req) {
        if (req.task() == null || req.task().isBlank()) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "任务描述不能为空");
        }
        AgentRunResult result = agentAppService.run(agentId, req.task());
        return Result.success(result);
    }

    /**
     * 查询 Agent 运行状态
     *
     * @param runId 运行记录 ID
     * @return Agent 运行状态和步骤详情
     */
    @GetMapping("/runs/{runId}")
    public Result<AgentRunResult> getRunStatus(@PathVariable Long runId) {
        AgentRunResult result = agentAppService.getRunStatus(runId);
        return Result.success(result);
    }

    /**
     * 创建 Agent 配置
     *
     * @param req Agent 配置信息
     * @return 创建的 Agent 信息
     */
    @PostMapping
    public Result<AgentResp> create(@RequestBody AgentCreateReq req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "Agent 名称不能为空");
        }

        AgentDO agentDO = new AgentDO();
        agentDO.setName(req.name());
        agentDO.setDescription(req.description());
        agentDO.setToolIds(serializeToolIds(req.toolIds()));
        agentDO.setSystemPrompt(req.systemPrompt());
        agentDO.setMaxSteps(req.maxSteps());
        agentMapper.insert(agentDO);

        AgentResp resp = new AgentResp(agentDO.getId(), agentDO.getName(),
                agentDO.getDescription(), req.toolIds(), agentDO.getSystemPrompt(),
                agentDO.getMaxSteps(), agentDO.getCreateTime() != null
                        ? agentDO.getCreateTime().toString() : null);
        return Result.success(resp);
    }

    private String serializeToolIds(List<Long> toolIds) {
        if (toolIds == null || toolIds.isEmpty()) {
            return "[]";
        }
        try {
            return objectMapper.writeValueAsString(toolIds);
        } catch (JsonProcessingException e) {
            return "[]";
        }
    }
}