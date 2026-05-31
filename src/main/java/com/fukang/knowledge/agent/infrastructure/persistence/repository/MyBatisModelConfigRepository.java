package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.application.model.port.ModelConfigRepository;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ModelConfigMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
@RequiredArgsConstructor
public class MyBatisModelConfigRepository implements ModelConfigRepository {

    private final ModelConfigMapper modelConfigMapper;

    @Override
    public void insert(ModelConfigDO config) {
        modelConfigMapper.insert(config);
    }

    @Override
    public ModelConfigDO findById(Long id) {
        return modelConfigMapper.selectById(id);
    }

    @Override
    public List<ModelConfigDO> findByProviderId(Long providerId) {
        return modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfigDO>().eq(ModelConfigDO::getProviderId, providerId));
    }

    @Override
    public List<ModelConfigDO> findByType(ModelTypeEnum modelType) {
        return modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfigDO>().eq(ModelConfigDO::getModelType, modelType.getCode()));
    }

    @Override
    public List<ModelConfigDO> findByProviderAndType(Long providerId, ModelTypeEnum modelType) {
        return modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfigDO>()
                        .eq(ModelConfigDO::getProviderId, providerId)
                        .eq(ModelConfigDO::getModelType, modelType.getCode()));
    }

    @Override
    public void updateById(ModelConfigDO config) {
        modelConfigMapper.updateById(config);
    }

    @Override
    public void deleteById(Long id) {
        modelConfigMapper.deleteById(id);
    }

    @Override
    public void deleteByProviderId(Long providerId) {
        modelConfigMapper.delete(
                new LambdaQueryWrapper<ModelConfigDO>().eq(ModelConfigDO::getProviderId, providerId));
    }
}
