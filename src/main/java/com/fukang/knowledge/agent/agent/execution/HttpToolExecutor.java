package com.fukang.knowledge.agent.agent.execution;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.agent.config.AgentProperties;
import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

/**
 * HTTP 工具执行器
 * <p>通过 RestTemplate 发送 HTTP GET/POST 请求调用外部 API。
 * 支持 URL 和 Header 中 {参数名} 占位符替换</p>
 */
@Slf4j
@Component
public class HttpToolExecutor implements ToolExecutor {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public HttpToolExecutor(AgentProperties agentProperties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) agentProperties.getToolTimeoutMs());
        factory.setReadTimeout((int) agentProperties.getToolTimeoutMs());
        this.restTemplate = new RestTemplate(factory);
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public ToolExecutionResult execute(ToolDefinition toolDefinition, Map<String, Object> parameters) {
        long start = System.currentTimeMillis();
        HttpToolConfig config = parseConfig(toolDefinition.executorConfig());

        try {
            String url = replacePlaceholders(config.url(), parameters);
            String method = config.method().toUpperCase();
            HttpHeaders headers = buildHeaders(config, parameters);

            String requestBody = null;
            if (config.body() != null && !config.body().isEmpty()) {
                requestBody = replacePlaceholders(objectMapper.writeValueAsString(config.body()), parameters);
            }

            HttpEntity<String> requestEntity = new HttpEntity<>(requestBody, headers);

            log.debug("HTTP 工具调用: method={}, url={}, headers={}", method, url, headers);

            ResponseEntity<String> response = switch (method) {
                case "GET" -> restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
                case "POST" -> restTemplate.exchange(url, HttpMethod.POST, requestEntity, String.class);
                case "PUT" -> restTemplate.exchange(url, HttpMethod.PUT, requestEntity, String.class);
                case "DELETE" -> restTemplate.exchange(url, HttpMethod.DELETE, requestEntity, String.class);
                default -> throw new IllegalArgumentException("不支持的 HTTP 方法: " + method);
            };

            long duration = System.currentTimeMillis() - start;
            String body = response.getBody() != null ? response.getBody() : "";
            boolean success = response.getStatusCode().is2xxSuccessful();

            if (!success) {
                log.warn("HTTP 工具返回非 2xx 状态码: tool={}, status={}, body={}",
                        toolDefinition.name(), response.getStatusCode(), body);
            }

            return success
                    ? ToolExecutionResult.success(body, duration)
                    : ToolExecutionResult.failure("HTTP " + response.getStatusCode() + ": " + body, duration);

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - start;
            log.error("HTTP 工具执行失败: tool={}, url={}", toolDefinition.name(), config.url(), e);
            return ToolExecutionResult.failure(e.getMessage(), duration);
        }
    }

    private HttpHeaders buildHeaders(HttpToolConfig config, Map<String, Object> parameters) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        if (config.headers() != null) {
            config.headers().forEach((key, value) ->
                    headers.set(key, replacePlaceholders(value, parameters)));
        }
        return headers;
    }

    private HttpToolConfig parseConfig(String executorConfig) {
        try {
            return objectMapper.readValue(executorConfig, HttpToolConfig.class);
        } catch (JsonProcessingException e) {
            throw new IllegalArgumentException("HTTP 工具配置解析失败: " + executorConfig, e);
        }
    }

    /**
     * 替换字符串中的 {参数名} 占位符
     */
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
     * HTTP 工具配置内部记录
     */
    private record HttpToolConfig(String url, String method, Map<String, String> headers,
                                   Map<String, Object> body) {
        public String url() {
            return url != null ? url : "";
        }

        public String method() {
            return method != null ? method : "GET";
        }
    }
}