package com.fukang.knowledge.agent.application.servicedesk.agent;

import com.fukang.knowledge.agent.infrastructure.tool.LocalMethodTool;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * 服务台补充信息工具：不写库，只生成需要用户补充的信息提示。
 */
@Component
public class ServiceDeskAskForMoreInfoTool implements LocalMethodTool {

    @Override
    public String name() {
        return ServiceDeskToolNames.ASK_FOR_MORE_INFO;
    }

    @Override
    public Object execute(Map<String, Object> arguments) {
        Object rawFields = arguments != null ? arguments.get("missingFields") : null;
        List<?> fields = rawFields instanceof List<?> list && !list.isEmpty()
                ? list
                : List.of("问题现象", "影响范围", "发生时间", "已尝试的处理方式");
        String message = text(arguments, "message",
                "为了更准确处理，请补充问题现象、影响范围、发生时间，以及你已经尝试过的处理方式。");
        return Map.of("status", "collect_info", "message", message, "missingFields", fields);
    }

    private String text(Map<String, Object> args, String key, String fallback) {
        Object value = args != null ? args.get(key) : null;
        return value != null && !String.valueOf(value).isBlank() ? String.valueOf(value) : fallback;
    }
}
