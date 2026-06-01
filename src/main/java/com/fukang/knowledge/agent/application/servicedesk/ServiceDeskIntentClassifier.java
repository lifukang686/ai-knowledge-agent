package com.fukang.knowledge.agent.application.servicedesk;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
import com.fukang.knowledge.agent.domain.servicedesk.ServiceDeskIntent;
import com.fukang.knowledge.agent.domain.servicedesk.ServiceType;
import com.fukang.knowledge.agent.domain.servicedesk.TicketPriority;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 服务台意图分类器。
 * <p>规则先兜住高确定性请求，LLM 用于补齐分类、标题和摘要等结构化信息。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ServiceDeskIntentClassifier {

    private static final String INTENT_TEMPLATE = "service-desk/intent-classifier.v1";
    private static final Pattern TICKET_NO_PATTERN = Pattern.compile("T\\d{18}");
    private static final Pattern CREATE_TICKET_PATTERN = Pattern.compile(
            "(报修|提交工单|创建工单|开通权限|无法|不能|连不上|打不开|失败|故障|坏了|异常|权限不足|账号锁定|密码错误|VPN|邮箱|电脑)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern QUERY_TICKET_PATTERN = Pattern.compile(
            "(查询|查看|进度|状态|处理到哪|处理了吗|我的工单|工单.*状态)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern HANDOFF_PATTERN = Pattern.compile(
            "(工资|薪资|绩效|劳动合同|仲裁|投诉|数据丢失|生产系统|安全事件|账号被盗)",
            Pattern.CASE_INSENSITIVE);
    private static final Pattern SUMMARY_PATTERN = Pattern.compile("(总结|概括|提炼|摘要).*(文档|文件|材料|内容)?");

    private final ChatCompletionPort chatCompletionPort;
    private final PromptTemplateManager promptTemplateManager;
    private final ObjectMapper objectMapper;

    public ServiceDeskDecision classify(String question, ServiceType preferredType) {
        ServiceDeskDecision ruleDecision = classifyByRule(question, preferredType);
        if (ruleDecision.intent() != ServiceDeskIntent.KNOWLEDGE_QA) {
            return ruleDecision;
        }
        try {
            String prompt = promptTemplateManager.renderText(INTENT_TEMPLATE, Map.of(
                    "question", question,
                    "serviceType", preferredType != null ? preferredType.name() : ServiceType.AUTO.name()));
            String json = chatCompletionPort.complete(
                    java.util.List.of(ChatCompletionPort.Message.user(prompt)));
            ServiceDeskDecision llmDecision = parseDecision(json, question, preferredType);
            log.debug("服务台 LLM 意图识别完成: intent={}, serviceType={}",
                    llmDecision.intent(), llmDecision.serviceType());
            return llmDecision;
        } catch (Exception e) {
            log.warn("服务台 LLM 意图识别失败，使用规则结果: question={}", question, e);
            return ruleDecision;
        }
    }

    private ServiceDeskDecision classifyByRule(String question, ServiceType preferredType) {
        String text = question != null ? question.trim() : "";
        ServiceType serviceType = normalizeServiceType(preferredType, text);
        if (TICKET_NO_PATTERN.matcher(text).find() || QUERY_TICKET_PATTERN.matcher(text).find()) {
            return decision(ServiceDeskIntent.QUERY_TICKET, serviceType, "工单查询", TicketPriority.MEDIUM,
                    titleFromQuestion(text), text, "命中工单查询规则");
        }
        if (HANDOFF_PATTERN.matcher(text).find()) {
            return decision(ServiceDeskIntent.HANDOFF_HUMAN, serviceType, "人工介入", TicketPriority.HIGH,
                    titleFromQuestion(text), text, "命中敏感或高风险规则");
        }
        if (SUMMARY_PATTERN.matcher(text).find()) {
            return decision(ServiceDeskIntent.SUMMARIZE_DOCUMENT, serviceType, "文档总结", TicketPriority.MEDIUM,
                    titleFromQuestion(text), text, "命中文档总结规则");
        }
        if (CREATE_TICKET_PATTERN.matcher(text).find()) {
            return decision(ServiceDeskIntent.CREATE_TICKET, serviceType, guessCategory(text), guessPriority(text),
                    titleFromQuestion(text), text, "命中创建工单规则");
        }
        return decision(ServiceDeskIntent.KNOWLEDGE_QA, serviceType, "知识问答", TicketPriority.MEDIUM,
                titleFromQuestion(text), text, "默认知识问答");
    }

    private ServiceDeskDecision parseDecision(String rawJson, String question, ServiceType preferredType) throws Exception {
        String json = extractJson(rawJson);
        Map<String, Object> map = objectMapper.readValue(json, new TypeReference<>() {});
        ServiceDeskIntent intent = ServiceDeskIntent.from(stringValue(map.get("intent")));
        ServiceType serviceType = ServiceType.from(stringValue(map.get("serviceType")));
        if (serviceType == ServiceType.AUTO) {
            serviceType = normalizeServiceType(preferredType, question);
        }
        TicketPriority priority = TicketPriority.from(stringValue(map.get("priority")));
        String category = nonBlank(stringValue(map.get("category")), guessCategory(question));
        String title = nonBlank(stringValue(map.get("title")), titleFromQuestion(question));
        String summary = nonBlank(stringValue(map.get("summary")), question);
        String reason = nonBlank(stringValue(map.get("reason")), "LLM 分类");
        return decision(intent, serviceType, category, priority, title, summary, reason);
    }

    private String extractJson(String text) {
        if (text == null) {
            return "{}";
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private ServiceDeskDecision decision(ServiceDeskIntent intent, ServiceType serviceType, String category,
                                         TicketPriority priority, String title, String summary, String reason) {
        return new ServiceDeskDecision(intent, serviceType, category, priority, title, summary, reason);
    }

    private ServiceType normalizeServiceType(ServiceType preferredType, String question) {
        if (preferredType != null && preferredType != ServiceType.AUTO) {
            return preferredType;
        }
        String text = question != null ? question.toLowerCase(Locale.ROOT) : "";
        if (text.contains("年假") || text.contains("调休") || text.contains("报销")
                || text.contains("薪资") || text.contains("工资") || text.contains("入职") || text.contains("离职")) {
            return ServiceType.HR;
        }
        if (text.contains("vpn") || text.contains("邮箱") || text.contains("电脑")
                || text.contains("账号") || text.contains("密码") || text.contains("网络")) {
            return ServiceType.IT;
        }
        return ServiceType.AUTO;
    }

    private String guessCategory(String question) {
        String text = question != null ? question.toLowerCase(Locale.ROOT) : "";
        if (text.contains("vpn") || text.contains("网络") || text.contains("连不上")) {
            return "网络";
        }
        if (text.contains("账号") || text.contains("密码") || text.contains("权限")) {
            return "账号权限";
        }
        if (text.contains("邮箱")) {
            return "邮箱";
        }
        if (text.contains("报销")) {
            return "报销";
        }
        if (text.contains("年假") || text.contains("调休") || text.contains("考勤")) {
            return "考勤休假";
        }
        return "综合";
    }

    private TicketPriority guessPriority(String question) {
        String text = question != null ? question : "";
        if (text.contains("紧急") || text.contains("生产") || text.contains("全部") || text.contains("无法办公")) {
            return TicketPriority.HIGH;
        }
        return TicketPriority.MEDIUM;
    }

    private String titleFromQuestion(String question) {
        String text = question != null ? question.trim().replaceAll("\\s+", " ") : "服务台请求";
        if (text.isBlank()) {
            return "服务台请求";
        }
        return text.length() > 60 ? text.substring(0, 60) : text;
    }

    private String stringValue(Object value) {
        return value != null ? String.valueOf(value).trim() : "";
    }

    private String nonBlank(String value, String fallback) {
        return value != null && !value.isBlank() ? value : fallback;
    }
}
