package com.fukang.knowledge.agent.api.agent;

import com.fukang.knowledge.agent.api.agent.dto.AgentCreateReq;
import com.fukang.knowledge.agent.api.agent.dto.AgentResp;
import com.fukang.knowledge.agent.api.agent.dto.AgentRunReq;
import com.fukang.knowledge.agent.application.agent.command.AgentCreateCommand;
import com.fukang.knowledge.agent.application.agent.result.AgentConfigResult;
import com.fukang.knowledge.agent.application.agent.AgentAppService;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.Result;
import com.fukang.knowledge.agent.domain.agent.model.AgentRunResult;
import com.fukang.knowledge.agent.domain.agent.model.ExecutionStrategy;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/agents")
@RequiredArgsConstructor
public class AgentController {

    private final AgentAppService agentAppService;

    @PostMapping("/{agentId}/run")
    public Result<AgentRunResult> run(@PathVariable Long agentId, @RequestBody AgentRunReq req) {
        if (req.task() == null || req.task().isBlank()) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "Task cannot be blank");
        }
        ExecutionStrategy strategy = ExecutionStrategy.from(req.executionStrategy());
        return Result.success(agentAppService.run(agentId, req.task(), strategy));
    }

    @GetMapping("/runs/{runId}")
    public Result<AgentRunResult> getRunStatus(@PathVariable Long runId) {
        return Result.success(agentAppService.getRunStatus(runId));
    }

    @PostMapping
    public Result<AgentResp> create(@RequestBody AgentCreateReq req) {
        AgentConfigResult result = agentAppService.create(new AgentCreateCommand(
                req.name(), req.description(), req.toolIds(), req.systemPrompt(), req.maxSteps()));
        return Result.success(new AgentResp(result.id(), result.name(), result.description(),
                result.toolIds(), result.systemPrompt(), result.maxSteps(), result.createTime()));
    }
}
