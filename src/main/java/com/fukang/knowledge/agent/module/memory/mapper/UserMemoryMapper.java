package com.fukang.knowledge.agent.module.memory.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.module.memory.model.entity.UserMemoryEntity;

import java.util.List;

/**
 * 用户记忆 Mapper。
 */
public interface UserMemoryMapper extends BaseMapper<UserMemoryEntity> {

    String STATUS_ACTIVE = "active";

    default List<UserMemoryEntity> findActiveByUser(Long userId, int limit) {
        return selectList(new LambdaQueryWrapper<UserMemoryEntity>()
                .eq(UserMemoryEntity::getUserId, userId)
                .eq(UserMemoryEntity::getStatus, STATUS_ACTIVE)
                .orderByDesc(UserMemoryEntity::getUpdateTime)
                .last("LIMIT " + limit));
    }

    default UserMemoryEntity findActiveByContent(Long userId, String memoryType, String content) {
        return selectOne(new LambdaQueryWrapper<UserMemoryEntity>()
                .eq(UserMemoryEntity::getUserId, userId)
                .eq(UserMemoryEntity::getMemoryType, memoryType)
                .eq(UserMemoryEntity::getContent, content)
                .eq(UserMemoryEntity::getStatus, STATUS_ACTIVE)
                .last("LIMIT 1"));
    }
}
