package com.fukang.knowledge.agent.module.conversation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.module.conversation.model.entity.ConversationSummaryEntity;

/**
 * RAG 会话摘要 Mapper。
 */
public interface ConversationSummaryMapper extends BaseMapper<ConversationSummaryEntity> {

    default ConversationSummaryEntity findLatestByConversationId(Long conversationId) {
        return selectOne(new LambdaQueryWrapper<ConversationSummaryEntity>()
                .eq(ConversationSummaryEntity::getConversationId, conversationId)
                .orderByDesc(ConversationSummaryEntity::getUpdateTime)
                .last("LIMIT 1"));
    }
}
