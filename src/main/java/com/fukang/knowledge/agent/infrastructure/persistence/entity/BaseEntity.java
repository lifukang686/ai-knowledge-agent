package com.fukang.knowledge.agent.infrastructure.persistence.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 基础实体类
 * <p>所有数据库实体的公共基类，定义主键、创建时间、更新时间和逻辑删除标志，
 * 配合 {@link com.fukang.knowledge.agent.infrastructure.config.MyBatisPlusConfig} 实现自动填充</p>
 */
@Data
public abstract class BaseEntity {

    /** 主键ID，使用雪花算法自动生成 */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /** 创建时间，插入时自动填充 */
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    /** 更新时间，插入和更新时自动填充 */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    /** 逻辑删除标志（0-未删除，1-已删除），插入时自动填充为 0 */
    @TableLogic
    @TableField(fill = FieldFill.INSERT)
    private Integer deleted;
}
