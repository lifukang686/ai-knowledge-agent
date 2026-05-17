package com.fukang.knowledge.agent.application.model;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.fukang.knowledge.agent.api.model.dto.ModelConfigReq;
import com.fukang.knowledge.agent.api.model.dto.ModelConfigUpdateReq;
import com.fukang.knowledge.agent.api.model.dto.ProviderReq;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.enums.ModelTypeEnum;
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
     * <p>创建前校验所属提供商是否存在以及模型类型是否合法</p>
     *
     * @param req 模型配置创建请求参数
     * @return 新创建的模型配置ID
     * @throws BaseException 提供商不存在时抛出 PROVIDER_NOT_EXIST
     * @throws BaseException 模型类型无效时抛出 MODEL_TYPE_INVALID
     */
    @Transactional(rollbackFor = Exception.class)
    public Long createModelConfig(ModelConfigReq req) {
        ModelProviderDO provider = providerMapper.selectById(req.providerId());
        if (provider == null) {
            throw new BaseException(ErrorCodeEnum.PROVIDER_NOT_EXIST);
        }

        validateModelType(req.modelType());

        ModelConfigDO config = new ModelConfigDO();
        config.setProviderId(req.providerId());
        config.setModelName(req.modelName());
        config.setModelType(req.modelType());
        if (req.defaultParams() != null && !req.defaultParams().isBlank()) {
            config.setDefaultParams(req.defaultParams());
        }
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

    /**
     * 根据ID删除模型配置
     * <p>删除前校验模型配置是否存在</p>
     *
     * @param id 模型配置ID
     * @throws BaseException 模型配置不存在时抛出 MODEL_NOT_EXIST
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteModelConfig(Long id) {
        ModelConfigDO config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw new BaseException(ErrorCodeEnum.MODEL_NOT_EXIST);
        }
        modelConfigMapper.deleteById(id);
    }

    /**
     * 根据ID更新模型配置
     * <p>更新前校验模型配置是否存在，仅更新请求中非空的字段</p>
     *
     * @param id  模型配置ID
     * @param req 模型配置更新请求参数
     * @throws BaseException 模型配置不存在时抛出 MODEL_NOT_EXIST
     * @throws BaseException 模型类型无效时抛出 MODEL_TYPE_INVALID
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateModelConfig(Long id, ModelConfigUpdateReq req) {
        ModelConfigDO config = modelConfigMapper.selectById(id);
        if (config == null) {
            throw new BaseException(ErrorCodeEnum.MODEL_NOT_EXIST);
        }
        if (req.providerId() != null) {
            config.setProviderId(req.providerId());
        }
        if (req.modelName() != null && !req.modelName().isBlank()) {
            config.setModelName(req.modelName());
        }
        if (req.modelType() != null && !req.modelType().isBlank()) {
            validateModelType(req.modelType());
            config.setModelType(req.modelType());
        }
        if (req.defaultParams() != null && !req.defaultParams().isBlank()) {
            config.setDefaultParams(req.defaultParams());
        }
        modelConfigMapper.updateById(config);
    }

    /**
     * 根据ID删除模型提供商
     * <p>同时级联删除该提供商下的所有模型配置数据</p>
     *
     * @param id 模型提供商ID
     * @throws BaseException 模型提供商不存在时抛出 PROVIDER_NOT_EXIST
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteProvider(Long id) {
        ModelProviderDO provider = providerMapper.selectById(id);
        if (provider == null) {
            throw new BaseException(ErrorCodeEnum.PROVIDER_NOT_EXIST);
        }

        LambdaQueryWrapper<ModelConfigDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ModelConfigDO::getProviderId, id);
        modelConfigMapper.delete(wrapper);

        providerMapper.deleteById(id);
    }

    /**
     * 根据ID更新模型提供商
     * <p>仅更新请求中非空的字段，未传的字段保持原值不变</p>
     *
     * @param id  模型提供商ID
     * @param req 模型提供商更新请求参数
     * @throws BaseException 模型提供商不存在时抛出 PROVIDER_NOT_EXIST
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateProvider(Long id, com.fukang.knowledge.agent.api.model.dto.ProviderUpdateReq req) {
        ModelProviderDO provider = providerMapper.selectById(id);
        if (provider == null) {
            throw new BaseException(ErrorCodeEnum.PROVIDER_NOT_EXIST);
        }
        if (req.name() != null && !req.name().isBlank()) {
            provider.setName(req.name());
        }
        if (req.apiBaseUrl() != null && !req.apiBaseUrl().isBlank()) {
            provider.setApiBaseUrl(req.apiBaseUrl());
        }
        if (req.apiKey() != null && !req.apiKey().isBlank()) {
            provider.setApiKey(req.apiKey());
        }
        if (req.description() != null && !req.description().isBlank()) {
            provider.setDescription(req.description());
        }
        providerMapper.updateById(provider);
    }

    /**
     * 设置默认模型提供商
     * <p>系统中只能存在一个默认提供商。设置新的默认提供商时，
     * 先取消所有现有提供商的默认状态，再设置目标提供商为默认。
     * 该操作在事务中执行以保证数据一致性。</p>
     *
     * @param id 模型提供商ID
     * @throws BaseException 模型提供商不存在时抛出 PROVIDER_NOT_EXIST
     */
    @Transactional(rollbackFor = Exception.class)
    public void setDefaultProvider(Long id) {
        ModelProviderDO provider = providerMapper.selectById(id);
        if (provider == null) {
            throw new BaseException(ErrorCodeEnum.PROVIDER_NOT_EXIST);
        }

        providerMapper.update(null,
                new LambdaUpdateWrapper<ModelProviderDO>()
                        .set(ModelProviderDO::getIsDefault, false));
        provider.setIsDefault(true);
        providerMapper.updateById(provider);

        log.info("已将模型提供商 [{}] 设为默认", provider.getName());
    }

    /**
     * 取消默认模型提供商
     *
     * @param id 模型提供商ID
     * @throws BaseException 模型提供商不存在时抛出 PROVIDER_NOT_EXIST
     */
    @Transactional(rollbackFor = Exception.class)
    public void cancelDefaultProvider(Long id) {
        ModelProviderDO provider = providerMapper.selectById(id);
        if (provider == null) {
            throw new BaseException(ErrorCodeEnum.PROVIDER_NOT_EXIST);
        }

        provider.setIsDefault(false);
        providerMapper.updateById(provider);

        log.info("已取消模型提供商 [{}] 的默认状态", provider.getName());
    }

    /**
     * 校验模型类型是否合法
     *
     * @param modelType 模型类型编码
     * @throws BaseException 模型类型无效时抛出 MODEL_TYPE_INVALID
     */
    private void validateModelType(String modelType) {
        try {
            ModelTypeEnum.fromCode(modelType);
        } catch (IllegalArgumentException e) {
            throw new BaseException(ErrorCodeEnum.MODEL_TYPE_INVALID);
        }
    }
}