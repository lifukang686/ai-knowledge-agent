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
public class MyBatisConversationMemoryRepository implements ConversationMemoryRepository {

    private final ConversationMapper conversationMapper;
    private final ConversationMessageMapper conversationMessageMapper;
    private final ConversationSummaryMapper conversationSummaryMapper;

    @Override
    public ConversationDO findConversationById(Long conversationId) {
        return conversationMapper.selectById(conversationId);
    }

    @Override
    public void insertConversation(ConversationDO conversation) {
        conversationMapper.insert(conversation);
    }

    @Override
    public void updateConversation(ConversationDO conversation) {
        conversationMapper.updateById(conversation);
    }

    @Override
    public void insertMessage(ConversationMessageDO message) {
        conversationMessageMapper.insert(message);
    }

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

    @Override
    public List<ConversationMessageDO> findAllMessages(Long conversationId) {
        LambdaQueryWrapper<ConversationMessageDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationMessageDO::getConversationId, conversationId)
                .orderByAsc(ConversationMessageDO::getCreateTime);
        return conversationMessageMapper.selectList(wrapper);
    }

    @Override
    public ConversationSummaryDO findLatestSummary(Long conversationId) {
        LambdaQueryWrapper<ConversationSummaryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ConversationSummaryDO::getConversationId, conversationId)
                .orderByDesc(ConversationSummaryDO::getUpdateTime)
                .last("LIMIT 1");
        return conversationSummaryMapper.selectOne(wrapper);
    }

    @Override
    public void insertSummary(ConversationSummaryDO summary) {
        conversationSummaryMapper.insert(summary);
    }

    @Override
    public void updateSummary(ConversationSummaryDO summary) {
        conversationSummaryMapper.updateById(summary);
    }
}
