package com.fukang.knowledge.agent.infrastructure.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.UserDO;

/**
 * 用户 Mapper 接口
 * <p>提供 sys_user 表的基础 CRUD 操作，由 MyBatis-Plus 自动实现</p>
 */
public interface UserMapper extends BaseMapper<UserDO> {
}
