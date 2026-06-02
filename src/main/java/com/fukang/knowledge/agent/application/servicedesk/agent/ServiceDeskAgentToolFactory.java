package com.fukang.knowledge.agent.application.servicedesk.agent;

import com.fukang.knowledge.agent.application.agent.tool.ScopedToolRegistry;
import com.fukang.knowledge.agent.common.enums.ExecutorTypeEnum;
import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 服务台 Agent 工具定义工厂。
 * <p>这些工具是服务台业务内置能力，只在服务台 Agent 的工具作用域内可见。</p>
 */
@Component
public class ServiceDeskAgentToolFactory {

    /**
     * 构造服务台专属工具作用域，避免复用全局动态工具造成越权。
     */
    public ScopedToolRegistry createScope() {
        return new ScopedToolRegistry(List.of(
                tool(ServiceDeskToolNames.KNOWLEDGE_QA,
                        "查询知识库并生成回答。适用于 IT/HR 制度、流程、操作说明等知识问答；不会创建工单。",
                        """
                                {"type":"object","properties":{"question":{"type":"string","description":"用户原始问题或需要检索的问题"}},"required":["question"]}
                                """),
                tool(ServiceDeskToolNames.DRAFT_TICKET,
                        "生成服务台工单草稿。适用于报修、权限申请、故障处理等写操作；只生成 DRAFT，必须用户确认后才正式打开。",
                        """
                                {"type":"object","properties":{"title":{"type":"string","description":"工单标题"},"category":{"type":"string","description":"工单分类"},"priority":{"type":"string","description":"LOW/MEDIUM/HIGH/URGENT"},"summary":{"type":"string","description":"Agent 整理的问题摘要"}},"required":["title","summary"]}
                                """),
                tool(ServiceDeskToolNames.QUERY_TICKET,
                        "查询当前用户的服务台工单。可按 ticketNo 查询；没有 ticketNo 时返回最近工单。",
                        """
                                {"type":"object","properties":{"ticketNo":{"type":"string","description":"可选，T 开头的工单号"}}}
                                """),
                tool(ServiceDeskToolNames.REQUEST_HUMAN_HANDOFF,
                        "请求人工介入。适用于高风险、敏感、投诉、安全事件等场景；只生成高优先级 DRAFT 工单。",
                        """
                                {"type":"object","properties":{"title":{"type":"string","description":"人工介入工单标题"},"category":{"type":"string","description":"分类"},"reason":{"type":"string","description":"需要人工介入的原因"}},"required":["title","reason"]}
                                """),
                tool(ServiceDeskToolNames.ASK_FOR_MORE_INFO,
                        "要求用户补充信息。适用于问题描述不足、文档总结暂不支持或无法确定下一步动作。",
                        """
                                {"type":"object","properties":{"missingFields":{"type":"array","items":{"type":"string"},"description":"需要用户补充的信息项"},"message":{"type":"string","description":"给用户的补充信息提示"}}}
                                """)
        ));
    }

    private ToolDefinition tool(String name, String description, String parametersSchema) {
        return new ToolDefinition(null, name, description, ExecutorTypeEnum.LOCAL_METHOD,
                "{}", parametersSchema, true);
    }
}
