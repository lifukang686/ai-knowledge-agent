package com.fukang.knowledge.agent.application.conversation.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationMessageDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ConversationSummaryDO;

import java.util.List;

/**
 * 会话记忆仓储端口，隔离应用层与 MyBatis 查询细节。
 */
public interface ConversationMemoryRepository {

    ConversationDO findConversationById(Long conversationId);

    void insertConversation(ConversationDO conversation);

    void updateConversation(ConversationDO conversation);

    void insertMessage(ConversationMessageDO message);

    List<ConversationMessageDO> findRecentMessages(Long conversationId, int limit);

    List<ConversationMessageDO> findAllMessages(Long conversationId);

    ConversationSummaryDO findLatestSummary(Long conversationId);

    void insertSummary(ConversationSummaryDO summary);

    void updateSummary(ConversationSummaryDO summary);
}
