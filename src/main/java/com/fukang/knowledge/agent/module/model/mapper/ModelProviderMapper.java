package com.fukang.knowledge.agent.module.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fukang.knowledge.agent.module.model.model.entity.ModelProviderEntity;

import java.util.List;

/**
 * 模型提供商 Mapper 接口
 * <p>提供 model_provider 表的基础 CRUD 操作，由 MyBatis-Plus 自动实现</p>
 */
public interface ModelProviderMapper extends BaseMapper<ModelProviderEntity> {

    default List<ModelProviderEntity> findAll() {
        return selectList(new LambdaQueryWrapper<>());
    }

    default ModelProviderEntity findDefault() {
        return selectOne(new LambdaQueryWrapper<ModelProviderEntity>().eq(ModelProviderEntity::getIsDefault, true));
    }

    default void clearDefault() {
        update(null, new LambdaUpdateWrapper<ModelProviderEntity>().set(ModelProviderEntity::getIsDefault, false));
    }
}
