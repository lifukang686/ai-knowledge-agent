package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 * <p>对应数据库表 sys_user，存储系统用户的基本信息和认证凭据</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("sys_user")
public class UserDO extends BaseEntity {

    /** 用户名，唯一标识 */
    private String username;

    /** 密码哈希值（当前 MVP 阶段为明文存储，后续需改为加密存储） */
    private String passwordHash;
}
