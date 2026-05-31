package com.fukang.knowledge.agent.infrastructure.ai;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;
import dev.langchain4j.data.segment.TextSegment;
import dev.langchain4j.model.output.Response;
import dev.langchain4j.model.scoring.ScoringModel;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 基于 HTTP 协议的 LangChain4j ScoringModel 实现。
 * <p>用于接入 OpenAI 兼容或常见 /rerank 风格的重排序服务。</p>
 */
public class HttpScoringModel implements ScoringModel {

    private static final Duration DEFAULT_TIMEOUT = Duration.ofSeconds(60);

    private final ModelProviderDO provider;
    private final ModelConfigDO modelConfig;
    private final RerankHttpOptions options;
    private final ObjectMapper objectMapper;
    private final HttpClient httpClient;

    public HttpScoringModel(ModelProviderDO provider, ModelConfigDO modelConfig, ObjectMapper objectMapper) {
        this.provider = provider;
        this.modelConfig = modelConfig;
        this.objectMapper = objectMapper;
        this.options = RerankHttpOptions.from(modelConfig, objectMapper);
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(options.timeout())
                .build();
    }

    @Override
    public Response<Double> score(String text, String query) {
        return score(TextSegment.from(text), query);
    }

    @Override
    public Response<Double> score(TextSegment segment, String query) {
        List<Double> scores = scoreAll(List.of(segment), query).content();
        return Response.from(scores.isEmpty() ? 0.0 : scores.getFirst());
    }

    @Override
    public Response<List<Double>> scoreAll(List<TextSegment> segments, String query) {
        try {
            HttpRequest request = buildRequest(query, segments);
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                throw new IllegalStateException("Rerank HTTP status=" + response.statusCode()
                        + ", body=" + response.body());
            }
            return Response.from(parseScores(response.body(), segments.size()));
        } catch (Exception e) {
            throw new IllegalStateException("Rerank scoring request failed", e);
        }
    }

    private HttpRequest buildRequest(String query, List<TextSegment> segments) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put(options.modelField(), modelConfig.getModelName());
        body.put(options.queryField(), query);
        body.put(options.documentsField(), segments.stream()
                .map(TextSegment::text)
                .map(text -> text != null ? text : "")
                .toList());
        body.put("top_n", segments.size());
        body.putAll(options.requestParams());

        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(resolveEndpoint(provider.getApiBaseUrl(), options.path())))
                .timeout(options.timeout())
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)));

        if (provider.getApiKey() != null && !provider.getApiKey().isBlank()) {
            builder.header("Authorization", "Bearer " + provider.getApiKey());
        }
        return builder.build();
    }

    private List<Double> parseScores(String responseBody, int segmentCount) throws Exception {
        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode scoresNode = root.get("scores");
        if (scoresNode != null && scoresNode.isArray()) {
            return parseScoreArray(scoresNode, segmentCount);
        }

        JsonNode resultsNode = root.get(options.resultsField());
        if (resultsNode == null && !"results".equals(options.resultsField())) {
            resultsNode = root.get("results");
        }
        if (resultsNode == null) {
            resultsNode = root.get("data");
        }
        if (resultsNode == null || !resultsNode.isArray()) {
            return List.of();
        }

        List<Double> scores = initScores(segmentCount);
        for (JsonNode item : resultsNode) {
            int index = item.path(options.indexField()).asInt(-1);
            if (index < 0) {
                index = item.path("index").asInt(-1);
            }
            JsonNode scoreNode = item.get(options.scoreField());
            if (scoreNode == null && !"score".equals(options.scoreField())) {
                scoreNode = item.get("score");
            }
            if (scoreNode == null) {
                scoreNode = item.get("relevance_score");
            }
            if (index >= 0 && index < segmentCount && scoreNode != null && scoreNode.isNumber()) {
                scores.set(index, scoreNode.asDouble());
            }
        }
        return scores;
    }

    private List<Double> parseScoreArray(JsonNode scoresNode, int segmentCount) {
        List<Double> scores = initScores(segmentCount);
        int count = Math.min(scoresNode.size(), segmentCount);
        for (int i = 0; i < count; i++) {
            JsonNode scoreNode = scoresNode.get(i);
            if (scoreNode != null && scoreNode.isNumber()) {
                scores.set(i, scoreNode.asDouble());
            }
        }
        return scores;
    }

    private List<Double> initScores(int segmentCount) {
        List<Double> scores = new ArrayList<>(segmentCount);
        for (int i = 0; i < segmentCount; i++) {
            scores.add(0.0);
        }
        return scores;
    }

    private String resolveEndpoint(String baseUrl, String path) {
        String normalizedBase = baseUrl != null ? baseUrl.trim() : "";
        if (normalizedBase.isBlank()) {
            normalizedBase = "https://api.openai.com/v1/";
        }
        String normalizedPath = path != null && !path.isBlank() ? path.trim() : "/rerank";
        if (normalizedPath.startsWith("http://") || normalizedPath.startsWith("https://")) {
            return normalizedPath;
        }
        if (normalizedBase.endsWith("/") && normalizedPath.startsWith("/")) {
            return normalizedBase + normalizedPath.substring(1);
        }
        if (!normalizedBase.endsWith("/") && !normalizedPath.startsWith("/")) {
            return normalizedBase + "/" + normalizedPath;
        }
        return normalizedBase + normalizedPath;
    }

    private record RerankHttpOptions(
            String path,
            String modelField,
            String queryField,
            String documentsField,
            String resultsField,
            String scoreField,
            String indexField,
            Duration timeout,
            Map<String, Object> requestParams
    ) {
        static RerankHttpOptions from(ModelConfigDO modelConfig, ObjectMapper objectMapper) {
            Map<String, Object> params = parseParams(modelConfig, objectMapper);
            return new RerankHttpOptions(
                    stringParam(params, "path", "/rerank"),
                    stringParam(params, "modelField", "model"),
                    stringParam(params, "queryField", "query"),
                    stringParam(params, "documentsField", "documents"),
                    stringParam(params, "resultsField", "results"),
                    stringParam(params, "scoreField", "relevance_score"),
                    stringParam(params, "indexField", "index"),
                    Duration.ofSeconds(longParam(params, "timeoutSeconds", DEFAULT_TIMEOUT.toSeconds())),
                    mapParam(params, "requestParams")
            );
        }

        private static Map<String, Object> parseParams(ModelConfigDO modelConfig, ObjectMapper objectMapper) {
            String raw = modelConfig.getDefaultParams();
            if (raw == null || raw.isBlank()) {
                return Map.of();
            }
            try {
                return objectMapper.readValue(raw, new TypeReference<Map<String, Object>>() {});
            } catch (Exception e) {
                return Map.of();
            }
        }

        private static String stringParam(Map<String, Object> params, String key, String defaultValue) {
            Object value = params.get(key);
            return value != null && !String.valueOf(value).isBlank() ? String.valueOf(value) : defaultValue;
        }

        private static long longParam(Map<String, Object> params, String key, long defaultValue) {
            Object value = params.get(key);
            if (value instanceof Number number) {
                return number.longValue();
            }
            if (value != null) {
                try {
                    return Long.parseLong(String.valueOf(value));
                } catch (NumberFormatException ignored) {
                    return defaultValue;
                }
            }
            return defaultValue;
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> mapParam(Map<String, Object> params, String key) {
            Object value = params.get(key);
            if (value instanceof Map<?, ?> map) {
                return (Map<String, Object>) map;
            }
            return Map.of();
        }
    }
}
