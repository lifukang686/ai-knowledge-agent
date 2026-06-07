package com.fukang.knowledge.agent.application.memory.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.UserMemoryDO;

import java.util.List;

/**
 * 用户记忆仓储端口。
 */
public interface UserMemoryRepository {

    List<UserMemoryDO> findActiveByUser(Long userId, int limit);

    UserMemoryDO findActiveByContent(Long userId, String memoryType, String content);

    void insert(UserMemoryDO memory);

    void update(UserMemoryDO memory);
}
