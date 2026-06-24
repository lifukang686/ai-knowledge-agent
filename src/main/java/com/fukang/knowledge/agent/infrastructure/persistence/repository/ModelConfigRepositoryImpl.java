package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.application.model.port.ModelConfigRepository;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ModelConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模型配置仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class ModelConfigRepositoryImpl implements ModelConfigRepository {

    private final ModelConfigMapper modelConfigMapper;

    /**
     * 新增模型配置。
     */
    @Override
    public void insert(ModelConfigDO config) {
        modelConfigMapper.insert(config);
    }

    /**
     * 按 ID 查询模型配置。
     */
    @Override
    public ModelConfigDO findById(Long id) {
        return modelConfigMapper.selectById(id);
    }

    /**
     * 查询提供商下的模型配置。
     */
    @Override
    public List<ModelConfigDO> findByProviderId(Long providerId) {
        return modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfigDO>().eq(ModelConfigDO::getProviderId, providerId));
    }

    /**
     * 按模型类型查询配置。
     */
    @Override
    public List<ModelConfigDO> findByType(ModelTypeEnum modelType) {
        return modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfigDO>().eq(ModelConfigDO::getModelType, modelType.getCode()));
    }

    /**
     * 按提供商和模型类型查询配置。
     */
    @Override
    public List<ModelConfigDO> findByProviderAndType(Long providerId, ModelTypeEnum modelType) {
        return modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfigDO>()
                        .eq(ModelConfigDO::getProviderId, providerId)
                        .eq(ModelConfigDO::getModelType, modelType.getCode()));
    }

    /**
     * 更新模型配置。
     */
    @Override
    public void updateById(ModelConfigDO config) {
        modelConfigMapper.updateById(config);
    }

    /**
     * 删除模型配置。
     */
    @Override
    public void deleteById(Long id) {
        modelConfigMapper.deleteById(id);
    }

    /**
     * 删除提供商下的全部模型配置。
     */
    @Override
    public void deleteByProviderId(Long providerId) {
        modelConfigMapper.delete(
                new LambdaQueryWrapper<ModelConfigDO>().eq(ModelConfigDO::getProviderId, providerId));
    }
}
