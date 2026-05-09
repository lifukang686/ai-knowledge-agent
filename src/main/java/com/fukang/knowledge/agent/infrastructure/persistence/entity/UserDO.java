package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户实体类
 * <p>对应数据库表 sys_user，存储系统用户的基本信息和认证凭据</p>
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Entity
@Table(name = "sys_user")
@TableName("sys_user")
public class UserDO extends BaseEntity {

    /** 用户名，唯一标识 */
    @Column(name = "username", nullable = false, unique = true, length = 64)
    private String username;

    /** 密码哈希值（当前 MVP 阶段为明文存储，后续需改为加密存储） */
    @Column(name = "password_hash", nullable = false, length = 128)
    private String passwordHash;
}
