package com.fukang.knowledge.agent.application.auth.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.UserDO;

/**
 * 用户仓储端口。
 */
public interface UserRepository {

    UserDO findByUsername(String username);

    void insert(UserDO user);
}
