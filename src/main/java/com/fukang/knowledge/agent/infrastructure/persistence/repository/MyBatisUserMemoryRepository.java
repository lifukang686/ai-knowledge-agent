package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.application.memory.port.UserMemoryRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.UserMemoryDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.UserMemoryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 用户记忆仓储的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisUserMemoryRepository implements UserMemoryRepository {

    private static final String STATUS_ACTIVE = "active";

    private final UserMemoryMapper userMemoryMapper;

    @Override
    public List<UserMemoryDO> findActiveByUser(Long userId, int limit) {
        LambdaQueryWrapper<UserMemoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMemoryDO::getUserId, userId)
                .eq(UserMemoryDO::getStatus, STATUS_ACTIVE)
                .orderByDesc(UserMemoryDO::getUpdateTime)
                .last("LIMIT " + limit);
        return userMemoryMapper.selectList(wrapper);
    }

    @Override
    public UserMemoryDO findActiveByContent(Long userId, String memoryType, String content) {
        LambdaQueryWrapper<UserMemoryDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserMemoryDO::getUserId, userId)
                .eq(UserMemoryDO::getMemoryType, memoryType)
                .eq(UserMemoryDO::getContent, content)
                .eq(UserMemoryDO::getStatus, STATUS_ACTIVE)
                .last("LIMIT 1");
        return userMemoryMapper.selectOne(wrapper);
    }

    @Override
    public void insert(UserMemoryDO memory) {
        userMemoryMapper.insert(memory);
    }

    @Override
    public void update(UserMemoryDO memory) {
        userMemoryMapper.updateById(memory);
    }
}
