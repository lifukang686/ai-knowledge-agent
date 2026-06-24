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
public class ModelProviderRepositoryImpl implements ModelProviderRepository {

    private final ModelProviderMapper providerMapper;

    /**
     * 新增模型提供商。
     */
    @Override
    public void insert(ModelProviderDO provider) {
        providerMapper.insert(provider);
    }

    /**
     * 查询全部模型提供商。
     */
    @Override
    public List<ModelProviderDO> findAll() {
        return providerMapper.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * 按 ID 查询模型提供商。
     */
    @Override
    public ModelProviderDO findById(Long id) {
        return providerMapper.selectById(id);
    }

    /**
     * 查询默认模型提供商。
     */
    @Override
    public ModelProviderDO findDefault() {
        return providerMapper.selectOne(
                new LambdaQueryWrapper<ModelProviderDO>().eq(ModelProviderDO::getIsDefault, true));
    }

    /**
     * 更新模型提供商。
     */
    @Override
    public void updateById(ModelProviderDO provider) {
        providerMapper.updateById(provider);
    }

    /**
     * 清除默认提供商标记。
     */
    @Override
    public void clearDefault() {
        providerMapper.update(null,
                new LambdaUpdateWrapper<ModelProviderDO>().set(ModelProviderDO::getIsDefault, false));
    }

    /**
     * 删除模型提供商。
     */
    @Override
    public void deleteById(Long id) {
        providerMapper.deleteById(id);
    }
}
