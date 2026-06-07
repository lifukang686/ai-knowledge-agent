package com.fukang.knowledge.agent.application.memory;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fukang.knowledge.agent.application.ai.port.ChatCompletionPort;
import com.fukang.knowledge.agent.application.memory.port.UserMemoryRepository;
import com.fukang.knowledge.agent.common.context.UserContextHolder;
import com.fukang.knowledge.agent.infrastructure.ai.PromptTemplateManager;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.UserMemoryDO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 用户级记忆服务，跨会话沉淀稳定偏好和事实。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserMemoryService {

    private static final String STATUS_ACTIVE = "active";
    private static final String EXTRACT_TEMPLATE = "rag/user-memory-extract.v1";
    private static final int MEMORY_LIMIT = 20;
    private static final int CONTENT_MAX_LENGTH = 300;
    private static final List<String> ALLOWED_TYPES = List.of("profile", "preference", "fact", "goal");

    private final UserMemoryRepository userMemoryRepository;
    private final ChatCompletionPort chatCompletionPort;
    private final PromptTemplateManager promptTemplateManager;
    private final ObjectMapper objectMapper;

    /**
     * 读取当前用户记忆，并格式化为 Prompt 文本。
     */
    public UserMemoryContext loadForCurrentUser() {
        Long userId = currentUserId();
        if (userId == null) {
            log.warn("读取用户记忆跳过: userId为空");
            return new UserMemoryContext(null, List.of(), "");
        }
        List<UserMemoryDO> memories = userMemoryRepository.findActiveByUser(userId, MEMORY_LIMIT);
        String promptText = formatPromptText(memories);
        log.info("读取用户记忆完成: userId={}, count={}", userId, memories.size());
        return new UserMemoryContext(userId, memories, promptText);
    }

    /**
     * 从成功问答中提取并更新用户记忆。
     *
     * @param conversationId  来源会话ID
     * @param sourceMessageId 来源助手消息ID
     * @param question        用户问题
     * @param answer          助手回答
     */
    @Transactional(rollbackFor = Exception.class)
    public void extractAndUpdate(Long conversationId, Long sourceMessageId, String question, String answer) {
        Long userId = currentUserId();
        if (userId == null) {
            log.warn("用户记忆更新跳过: userId为空, conversationId={}, messageId={}",
                    conversationId, sourceMessageId);
            return;
        }
        if (!StringUtils.hasText(question) || !StringUtils.hasText(answer)) {
            return;
        }

        List<MemoryCandidate> candidates = extractCandidates(question, answer);
        int inserted = 0;
        int updated = 0;
        for (MemoryCandidate candidate : candidates) {
            if (!isValidCandidate(candidate)) {
                continue;
            }
            UserMemoryDO existing = userMemoryRepository.findActiveByContent(
                    userId, candidate.type(), candidate.content().trim());
            if (existing == null) {
                userMemoryRepository.insert(toMemory(userId, conversationId, sourceMessageId, candidate));
                inserted++;
            } else {
                existing.setConfidence(Math.max(existing.getConfidence(), normalizeConfidence(candidate.confidence())));
                existing.setSourceConversationId(conversationId);
                existing.setSourceMessageId(sourceMessageId);
                userMemoryRepository.update(existing);
                updated++;
            }
        }
        log.info("更新用户记忆完成: userId={}, candidateCount={}, inserted={}, updated={}",
                userId, candidates.size(), inserted, updated);
    }

    /**
     * 提取本轮候选记忆。
     *
     * @param question 用户问题
     * @param answer   助手回答
     */
    private List<MemoryCandidate> extractCandidates(String question, String answer) {
        try {
            String response = chatCompletionPort.complete(List.of(
                    ChatCompletionPort.Message.system("你是用户记忆提取助手，只输出 JSON 数组。"),
                    ChatCompletionPort.Message.user(buildExtractPrompt(question, answer))
            ));
            String json = extractJsonArray(response);
            if (!StringUtils.hasText(json)) {
                return List.of();
            }
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            log.warn("用户记忆提取失败，跳过本轮: {}", e.getMessage());
            return List.of();
        }
    }

    /**
     * 构造记忆提取 Prompt。
     *
     * @param question 用户问题
     * @param answer   助手回答
     */
    private String buildExtractPrompt(String question, String answer) {
        return promptTemplateManager.renderText(EXTRACT_TEMPLATE, Map.of(
                "question", question != null ? question : "",
                "answer", answer != null ? answer : ""
        ));
    }

    /**
     * 从模型输出中截取 JSON 数组。
     *
     * @param text 模型输出文本
     */
    private String extractJsonArray(String text) {
        if (!StringUtils.hasText(text)) {
            return "";
        }
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end < start) {
            return "";
        }
        return text.substring(start, end + 1);
    }

    /**
     * 校验候选记忆是否可保存。
     *
     * @param candidate 候选记忆
     */
    private boolean isValidCandidate(MemoryCandidate candidate) {
        return candidate != null
                && ALLOWED_TYPES.contains(candidate.type())
                && StringUtils.hasText(candidate.content())
                && candidate.content().length() <= CONTENT_MAX_LENGTH;
    }

    /**
     * 转换为用户记忆实体。
     *
     * @param userId          用户ID
     * @param conversationId  来源会话ID
     * @param sourceMessageId 来源消息ID
     * @param candidate       候选记忆
     */
    private UserMemoryDO toMemory(Long userId, Long conversationId, Long sourceMessageId, MemoryCandidate candidate) {
        UserMemoryDO memory = new UserMemoryDO();
        memory.setUserId(userId);
        memory.setMemoryType(candidate.type());
        memory.setContent(candidate.content().trim());
        memory.setConfidence(normalizeConfidence(candidate.confidence()));
        memory.setSourceConversationId(conversationId);
        memory.setSourceMessageId(sourceMessageId);
        memory.setStatus(STATUS_ACTIVE);
        return memory;
    }

    /**
     * 格式化为 Prompt 记忆块。
     *
     * @param memories 用户记忆列表
     */
    private String formatPromptText(List<UserMemoryDO> memories) {
        if (memories == null || memories.isEmpty()) {
            return "";
        }
        Map<String, StringBuilder> grouped = new LinkedHashMap<>();
        grouped.put("profile", new StringBuilder());
        grouped.put("preference", new StringBuilder());
        grouped.put("fact", new StringBuilder());
        grouped.put("goal", new StringBuilder());

        for (UserMemoryDO memory : memories) {
            StringBuilder builder = grouped.get(memory.getMemoryType());
            if (builder != null && StringUtils.hasText(memory.getContent())) {
                builder.append("- ").append(memory.getContent()).append("\n");
            }
        }

        StringBuilder prompt = new StringBuilder();
        appendGroup(prompt, "用户画像", grouped.get("profile"));
        appendGroup(prompt, "用户偏好", grouped.get("preference"));
        appendGroup(prompt, "用户事实", grouped.get("fact"));
        appendGroup(prompt, "近期目标", grouped.get("goal"));
        return prompt.toString().trim();
    }

    /**
     * 追加一组记忆内容。
     *
     * @param prompt  Prompt 文本
     * @param title   分组标题
     * @param content 分组内容
     */
    private void appendGroup(StringBuilder prompt, String title, StringBuilder content) {
        if (content == null || content.isEmpty()) {
            return;
        }
        prompt.append("【").append(title).append("】\n").append(content);
    }

    /**
     * 归一化记忆置信度。
     *
     * @param confidence 原始置信度
     */
    private double normalizeConfidence(Double confidence) {
        if (confidence == null) {
            return 0.8D;
        }
        return Math.max(0.1D, Math.min(1D, confidence));
    }

    private Long currentUserId() {
        Long userId = UserContextHolder.getUserId();
        return userId;
    }

    private record MemoryCandidate(String type, String content, Double confidence) {}
}
