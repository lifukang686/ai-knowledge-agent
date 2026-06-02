package com.fukang.knowledge.agent.infrastructure.persistence.repository;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fukang.knowledge.agent.application.model.port.ModelProviderRepository;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ModelProviderMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * 模型提供商仓储端口的 MyBatis 实现。
 */
@Repository
@RequiredArgsConstructor
public class MyBatisModelProviderRepository implements ModelProviderRepository {

    private final ModelProviderMapper providerMapper;

    @Override
    public void insert(ModelProviderDO provider) {
        providerMapper.insert(provider);
    }

    @Override
    public List<ModelProviderDO> findAll() {
        return providerMapper.selectList(new LambdaQueryWrapper<>());
    }

    @Override
    public ModelProviderDO findById(Long id) {
        return providerMapper.selectById(id);
    }

    @Override
    public ModelProviderDO findDefault() {
        return providerMapper.selectOne(
                new LambdaQueryWrapper<ModelProviderDO>().eq(ModelProviderDO::getIsDefault, true));
    }

    @Override
    public void updateById(ModelProviderDO provider) {
        providerMapper.updateById(provider);
    }

    @Override
    public void clearDefault() {
        providerMapper.update(null,
                new LambdaUpdateWrapper<ModelProviderDO>().set(ModelProviderDO::getIsDefault, false));
    }

    @Override
    public void deleteById(Long id) {
        providerMapper.deleteById(id);
    }
}
