package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.application.auth.port.UserRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.UserDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

/**
 * 用户仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisUserRepository implements UserRepository {

    private final UserMapper userMapper;

    @Override
    public UserDO findByUsername(String username) {
        return userMapper.selectOne(
                new LambdaQueryWrapper<UserDO>().eq(UserDO::getUsername, username));
    }

    @Override
    public void insert(UserDO user) {
        userMapper.insert(user);
    }
}
