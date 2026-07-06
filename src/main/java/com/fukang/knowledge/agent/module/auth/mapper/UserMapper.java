package com.fukang.knowledge.agent.module.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.module.auth.model.entity.UserEntity;

/**
 * 用户 Mapper 接口
 * <p>提供 sys_user 表的基础 CRUD 操作，由 MyBatis-Plus 自动实现</p>
 */
public interface UserMapper extends BaseMapper<UserEntity> {

    default UserEntity findByUsername(String username) {
        return selectOne(new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username));
    }
}
