package com.fukang.knowledge.agent.infrastructure.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.request.json.JsonArraySchema;
import dev.langchain4j.model.chat.request.json.JsonBooleanSchema;
import dev.langchain4j.model.chat.request.json.JsonIntegerSchema;
import dev.langchain4j.model.chat.request.json.JsonNumberSchema;
import dev.langchain4j.model.chat.request.json.JsonObjectSchema;
import dev.langchain4j.model.chat.request.json.JsonSchemaElement;
import dev.langchain4j.model.chat.request.json.JsonStringSchema;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 工具参数 Schema 转换器。
 * <p>将平台内保存的 JSON Schema 转换为 LangChain4j ToolSpecification 可识别的
 * JsonObjectSchema，让模型不再只依赖自然语言 description 猜测参数。</p>
 */
@Slf4j
@Component
public class ToolSchemaConverter {

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * 解析工具参数 JSON Schema。
     * <p>兼容两种格式：根节点就是 object schema，或根节点下包含 parameters object。</p>
     */
    public JsonObjectSchema fromJsonSchema(String schemaJson) {
        if (!StringUtils.hasText(schemaJson)) {
            return JsonObjectSchema.builder().properties(Map.of()).required(List.of()).build();
        }
        try {
            JsonNode root = objectMapper.readTree(schemaJson);
            JsonNode objectNode = "object".equals(typeOf(root)) ? root : root.path("parameters");
            if (!objectNode.isObject()) {
                return JsonObjectSchema.builder().properties(Map.of()).required(List.of()).build();
            }
            return toObjectSchema(objectNode);
        } catch (Exception e) {
            log.warn("Failed to parse tool parameter schema, fallback to empty object schema: {}", schemaJson, e);
            return JsonObjectSchema.builder().properties(Map.of()).required(List.of()).build();
        }
    }

    private JsonObjectSchema toObjectSchema(JsonNode node) {
        Map<String, JsonSchemaElement> properties = new LinkedHashMap<>();
        JsonNode props = node.path("properties");
        if (props.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = props.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                properties.put(field.getKey(), toSchemaElement(field.getValue()));
            }
        }
        return JsonObjectSchema.builder()
                .description(text(node, "description"))
                .properties(properties)
                .required(required(node.path("required")))
                .additionalProperties(false)
                .build();
    }

    /** 递归转换 JSON Schema 属性，当前覆盖常见基础类型与 object/array。 */
    private JsonSchemaElement toSchemaElement(JsonNode node) {
        String description = text(node, "description");
        return switch (typeOf(node)) {
            case "integer" -> JsonIntegerSchema.builder().description(description).build();
            case "number" -> JsonNumberSchema.builder().description(description).build();
            case "boolean" -> JsonBooleanSchema.builder().description(description).build();
            case "array" -> JsonArraySchema.builder()
                    .description(description)
                    .items(toSchemaElement(node.path("items")))
                    .build();
            case "object" -> toObjectSchema(node);
            default -> JsonStringSchema.builder().description(description).build();
        };
    }

    /** JSON Schema type 可能是字符串或数组，这里统一取第一个有效类型。 */
    private String typeOf(JsonNode node) {
        JsonNode type = node.path("type");
        if (type.isTextual()) {
            return type.asText();
        }
        if (type.isArray() && !type.isEmpty() && type.get(0).isTextual()) {
            return type.get(0).asText();
        }
        return "string";
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node.path(field);
        return value.isTextual() ? value.asText() : null;
    }

    private List<String> required(JsonNode node) {
        if (!node.isArray()) {
            return List.of();
        }
        List<String> required = new ArrayList<>();
        node.forEach(item -> {
            if (item.isTextual()) {
                required.add(item.asText());
            }
        });
        return required;
    }
}
