package com.fukang.knowledge.agent.application.model.port;

import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;

import java.util.List;

/**
 * 模型提供商仓储端口。
 */
public interface ModelProviderRepository {

    void insert(ModelProviderDO provider);

    List<ModelProviderDO> findAll();

    ModelProviderDO findById(Long id);

    ModelProviderDO findDefault();

    void updateById(ModelProviderDO provider);

    void clearDefault();

    void deleteById(Long id);
}
