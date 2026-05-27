package com.fukang.knowledge.agent.infrastructure.data;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.common.enums.ExecutorTypeEnum;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.AgentDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ToolDefinitionDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.AgentMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ToolDefinitionMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Agent 示例数据初始化器
 * <p>应用启动时自动注册示例工具和 Agent 配置。
 * 仅在表为空时执行初始化，已存在数据时跳过</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AgentDataInitializer implements CommandLineRunner {

    private final ToolDefinitionMapper toolDefinitionMapper;
    private final AgentMapper agentMapper;

    @Override
    public void run(String... args) {
        initTools();
        initAgent();
    }

    private void initTools() {
        Long count = toolDefinitionMapper.selectCount(
                new LambdaQueryWrapper<ToolDefinitionDO>()
                        .eq(ToolDefinitionDO::getName, "queryDatabase"));

        if (count > 0) {
            log.info("示例工具已存在，跳过初始化");
            return;
        }

        ToolDefinitionDO sqlTool = new ToolDefinitionDO();
        sqlTool.setName("queryDatabase");
        sqlTool.setDescription("查询数据库表数据，执行 SELECT 语句并返回 JSON 格式结果。"
                + "适用于查询任意已存在的数据库表，支持 WHERE 条件过滤");
        sqlTool.setExecutorType(ExecutorTypeEnum.SQL.getCode());
        sqlTool.setExecutorConfig("{\"sql\":\"SELECT * FROM {table} WHERE 1=1\"}");
        sqlTool.setParametersSchema("{\"table\": \"string，要查询的数据库表名\"}");
        sqlTool.setEnabled(true);
        toolDefinitionMapper.insert(sqlTool);

        ToolDefinitionDO httpTool = new ToolDefinitionDO();
        httpTool.setName("fetchApiData");
        httpTool.setDescription("通过 HTTP GET 请求获取外部 API 数据。"
                + "适用于调用第三方 REST API 获取 JSON 格式数据");
        httpTool.setExecutorType(ExecutorTypeEnum.HTTP.getCode());
        httpTool.setExecutorConfig("{\"url\":\"{apiUrl}\",\"method\":\"GET\"}");
        httpTool.setParametersSchema("{\"apiUrl\": \"string，完整的API请求URL，如 http://example.com/api/data\"}");
        httpTool.setEnabled(true);
        toolDefinitionMapper.insert(httpTool);

        log.info("示例工具注册完成: queryDatabase (SQL), fetchApiData (HTTP)");
    }

    private void initAgent() {
        Long count = agentMapper.selectCount(
                new LambdaQueryWrapper<AgentDO>()
                        .eq(AgentDO::getName, "数据分析助手"));

        if (count > 0) {
            log.info("示例 Agent 已存在，跳过初始化");
            return;
        }

        List<ToolDefinitionDO> tools = toolDefinitionMapper.selectList(
                new LambdaQueryWrapper<ToolDefinitionDO>()
                        .eq(ToolDefinitionDO::getName, "queryDatabase"));

        AgentDO agentDO = new AgentDO();
        agentDO.setName("数据分析助手");
        agentDO.setDescription("通用数据分析 Agent，能够查询数据库表并综合分析数据");
        if (!tools.isEmpty()) {
            agentDO.setToolIds("[" + tools.get(0).getId() + "]");
        }
        agentDO.setSystemPrompt("你是一个数据分析专家，帮助用户分析数据并生成报告");
        agentDO.setMaxSteps(5);
        agentMapper.insert(agentDO);

        log.info("示例 Agent 注册完成: id={}, name=数据分析助手", agentDO.getId());
    }
}