package com.fukang.knowledge.agent.application.model;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fukang.knowledge.agent.api.model.dto.ModelConfigReq;
import com.fukang.knowledge.agent.api.model.dto.ProviderReq;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ModelConfigMapper;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.ModelProviderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * 模型管理应用服务
 * <p>处理模型提供商和模型配置的增删查业务逻辑</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelAppService {

    private final ModelProviderMapper providerMapper;
    private final ModelConfigMapper modelConfigMapper;

    /**
     * 创建模型提供商
     *
     * @param req 提供商创建请求参数
     * @return 新创建的提供商ID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createProvider(ProviderReq req) {
        ModelProviderDO provider = new ModelProviderDO();
        provider.setName(req.name());
        provider.setApiBaseUrl(req.apiBaseUrl());
        provider.setApiKey(req.apiKey());
        provider.setDescription(req.description());
        providerMapper.insert(provider);
        return provider.getId();
    }

    /**
     * 查询所有模型提供商
     *
     * @return 提供商列表
     */
    public List<ModelProviderDO> listProviders() {
        return providerMapper.selectList(new LambdaQueryWrapper<>());
    }

    /**
     * 创建模型配置
     * <p>创建前校验所属提供商是否存在，不存在则抛出异常</p>
     *
     * @param req 模型配置创建请求参数
     * @return 新创建的模型配置ID
     * @throws BaseException 提供商不存在时抛出 PROVIDER_NOT_EXIST
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createModelConfig(ModelConfigReq req) {
        // 校验提供商是否存在
        ModelProviderDO provider = providerMapper.selectById(req.providerId());
        if (provider == null) {
            throw new BaseException(ErrorCodeEnum.PROVIDER_NOT_EXIST);
        }

        ModelConfigDO config = new ModelConfigDO();
        config.setProviderId(req.providerId());
        config.setModelName(req.modelName());
        config.setDefaultParams(req.defaultParams());
        modelConfigMapper.insert(config);
        return config.getId();
    }

    /**
     * 根据提供商ID查询模型配置列表
     *
     * @param providerId 提供商ID
     * @return 该提供商下的模型配置列表
     */
    public List<ModelConfigDO> listModelConfigs(Long providerId) {
        return modelConfigMapper.selectList(
                new LambdaQueryWrapper<ModelConfigDO>()
                        .eq(ModelConfigDO::getProviderId, providerId)
        );
    }
}
