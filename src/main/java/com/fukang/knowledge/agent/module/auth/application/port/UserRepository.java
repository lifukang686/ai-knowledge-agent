package com.fukang.knowledge.agent.module.auth.application.port;

import com.fukang.knowledge.agent.module.auth.infrastructure.persistence.entity.UserDO;

/**
 * 用户仓储端口。
 */
public interface UserRepository {

    UserDO findByUsername(String username);

    void insert(UserDO user);
}
