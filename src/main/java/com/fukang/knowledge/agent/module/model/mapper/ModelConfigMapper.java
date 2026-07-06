package com.fukang.knowledge.agent.module.model.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.module.model.model.entity.ModelConfigEntity;

import java.util.List;

/**
 * 模型配置 Mapper 接口
 * <p>提供 model_config 表的基础 CRUD 操作，由 MyBatis-Plus 自动实现</p>
 */
public interface ModelConfigMapper extends BaseMapper<ModelConfigEntity> {

    default List<ModelConfigEntity> findByProviderId(Long providerId) {
        return selectList(new LambdaQueryWrapper<ModelConfigEntity>().eq(ModelConfigEntity::getProviderId, providerId));
    }

    default List<ModelConfigEntity> findByType(ModelTypeEnum modelType) {
        return selectList(new LambdaQueryWrapper<ModelConfigEntity>().eq(ModelConfigEntity::getModelType, modelType.getCode()));
    }

    default List<ModelConfigEntity> findByProviderAndType(Long providerId, ModelTypeEnum modelType) {
        return selectList(new LambdaQueryWrapper<ModelConfigEntity>()
                .eq(ModelConfigEntity::getProviderId, providerId)
                .eq(ModelConfigEntity::getModelType, modelType.getCode()));
    }

    default void deleteByProviderId(Long providerId) {
        delete(new LambdaQueryWrapper<ModelConfigEntity>().eq(ModelConfigEntity::getProviderId, providerId));
    }
}
