package com.fukang.knowledge.agent.infrastructure.tool;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;
import com.fukang.knowledge.agent.domain.agent.model.ToolExecutionResult;
import com.fukang.knowledge.agent.domain.agent.service.ToolExecutor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * SQL 工具执行器
 * <p>通过 JdbcTemplate 执行数据库查询操作（只读 SELECT），
 * 支持 SQL 中 {参数名} 占位符替换，结果以 JSON 数组格式返回</p>
 */
@Slf4j
@Component
public class SqlToolExecutor implements ToolExecutor {

    private final JdbcTemplate jdbcTemplate;
    private final ObjectMapper objectMapper;

    public SqlToolExecutor(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ToolExecutionResult execute(ToolDefinition toolDefinition, Map<String, Object> parameters) {
        long start = System.currentTimeMillis();

        try {
            SqlToolConfig config = parseConfig(toolDefinition.executorConfig());
            String sql = replacePlaceholders(config.sql(), parameters);

            log.debug("SQL 工具执行: tool={}, sql={}, params={}", toolDefinition.name(), sql, parameters);

            List<Map<String, Object>> results = jdbcTemplate.queryForList(sql);
            String jsonResult = objectMapper.writeValueAsString(results);

            long duration = System.currentTimeMillis() - start;
            return ToolExecutionResult.success(jsonResult, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("SQL 工具执行失败: tool={}", toolDefinition.name(), e);
            return ToolExecutionResult.failure(e.getMessage(), duration);
        }
    }

    private SqlToolConfig parseConfig(String executorConfig) {
        try {
            return objectMapper.readValue(executorConfig, SqlToolConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("SQL 工具配置解析失败: " + executorConfig, e);
        }
    }

    private String replacePlaceholders(String template, Map<String, Object> parameters) {
        if (template == null) {
            return null;
        }
        String result = template;
        for (Map.Entry<String, Object> entry : parameters.entrySet()) {
            result = result.replace("{" + entry.getKey() + "}", String.valueOf(entry.getValue()));
        }
        return result;
    }

    /**
     * SQL 工具配置内部记录
     */
    private record SqlToolConfig(String sql) {
        public String sql() {
            return sql != null ? sql : "";
        }
    }
}