package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.application.conversation.port.ConversationMemoryRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationMessageDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationSummaryDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ConversationMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ConversationMessageMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ConversationSummaryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.Comparator;
import java.util.List;

/**
 * 会话记忆仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class ConversationMemoryRepositoryImpl implements ConversationMemoryRepository {

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;

    /**
     * 按 ID 查询会话。
     */
    @Override
    public ConversationDO findConversationById(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    /**
     * 查询用户会话列表。
     */
    @Override
    public List<ConversationDO> findConversationsByUser(Long userId, Long knowledgeBaseId, int limit) {
        LambdaQueryWrapper<ConversationDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationDO::getUserId, userId)
                .eq(ConversationDO::getStatus, "active");
        if (knowledgeBaseId != null) {
            wrapper.eq(ConversationDO::getKnowledgeBaseId, knowledgeBaseId);
        }
        wrapper.orderByDesc(ConversationDO::getUpdateTime)
                .last("LIMIT " + limit);
        return conversationMapper.selectList(wrapper);
    }

    /**
     * 统计会话消息数。
     */
    @Override
    public long countMessages(Long conversationId) {
        LambdaQueryWrapper<ConversationMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessageDO::getConversationId, conversationId);
        return conversationMessageMapper.selectCount(wrapper);
    }

    /**
     * 新增会话。
     */
    @Override
    public void insertConversation(ConversationDO conversation) {
        conversationMapper.insert(conversation);
    }

    /**
     * 更新会话。
     */
    @Override
    public void updateConversation(ConversationDO conversation) {
        conversationMapper.updateById(conversation);
    }

    /**
     * 新增会话消息。
     */
    @Override
    public void insertMessage(ConversationMessageDO message) {
        conversationMessageMapper.insert(message);
    }

    /**
     * 查询最近消息并按时间正序返回。
     */
    @Override
    public List<ConversationMessageDO> findRecentMessages(Long conversationId, int limit) {
        LambdaQueryWrapper<ConversationMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessageDO::getConversationId, conversationId)
                .orderByDesc(ConversationMessageDO::getCreateTime)
                .last("LIMIT " + limit);
        List<ConversationMessageDO> messages = conversationMessageMapper.selectList(wrapper);
        return messages.stream()
                .sorted(Comparator.comparing(ConversationMessageDO::getCreateTime))
                .toList();
    }

    /**
     * 查询会话全部消息。
     */
    @Override
    public List<ConversationMessageDO> findAllMessages(Long conversationId) {
        LambdaQueryWrapper<ConversationMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessageDO::getConversationId, conversationId)
                .orderByAsc(ConversationMessageDO::getCreateTime);
        return conversationMessageMapper.selectList(wrapper);
    }

    /**
     * 查询最新会话摘要。
     */
    @Override
    public ConversationSummaryDO findLatestSummary(Long conversationId) {
        LambdaQueryWrapper<ConversationSummaryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationSummaryDO::getConversationId, conversationId)
                .orderByDesc(ConversationSummaryDO::getUpdateTime)
                .last("LIMIT 1");
        return conversationSummaryMapper.selectOne(wrapper);
    }

    /**
     * 新增会话摘要。
     */
    @Override
    public void insertSummary(ConversationSummaryDO summary) {
        conversationSummaryMapper.insert(summary);
    }

    /**
     * 更新会话摘要。
     */
    @Override
    public void updateSummary(ConversationSummaryDO summary) {
        conversationSummaryMapper.updateById(summary);
    }
}
