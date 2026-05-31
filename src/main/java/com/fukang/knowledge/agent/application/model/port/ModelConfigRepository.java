package com.fukang.knowledge.agent.application.model.port;

import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;

import java.util.List;

/**
 * 模型配置仓储端口。
 */
public interface ModelConfigRepository {

    void insert(ModelConfigDO config);

    ModelConfigDO findById(Long id);

    List<ModelConfigDO> findByProviderId(Long providerId);

    List<ModelConfigDO> findByType(ModelTypeEnum modelType);

    List<ModelConfigDO> findByProviderAndType(Long providerId, ModelTypeEnum modelType);

    void updateById(ModelConfigDO config);

    void deleteById(Long id);

    void deleteByProviderId(Long providerId);
}
