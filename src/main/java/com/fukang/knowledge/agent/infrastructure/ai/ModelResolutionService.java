package com.fukang.knowledge.agent.infrastructure.ai;

import com.fukang.knowledge.agent.application.model.ModelAppService;
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

import java.util.List;

/**
 * 模型配置解析服务
 * <p>负责从数据库解析模型提供商和模型配置的策略：
 * 优先使用默认提供商 → 其次使用任意匹配类型的提供商</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ModelResolutionService {

    private final ModelAppService modelAppService;
    private final ModelProviderMapper providerMapper;
    private final ModelConfigMapper modelConfigMapper;

    /**
     * 解析可用的模型提供商
     * <p>策略：优先返回默认提供商；若无默认提供商，取列表中第一个</p>
     *
     * @return 模型提供商
     */
    public ModelProviderDO resolveProvider() {
        ModelProviderDO defaultProvider = modelAppService.findDefaultProvider();
        if (defaultProvider != null) {
            log.debug("解析到默认模型提供商: {}", defaultProvider.getName());
            return defaultProvider;
        }
        List<ModelProviderDO> allProviders = modelAppService.listProviders();
        if (allProviders.isEmpty()) {
            log.error("系统中未配置任何模型提供商");
            throw new BaseException(ErrorCodeEnum.NO_MODEL_PROVIDER_AVAILABLE);
        }
        log.info("无默认提供商，使用第一个可用提供商: {}", allProviders.get(0).getName());
        return allProviders.get(0);
    }

    /**
     * 解析指定提供商下指定类型的模型配置
     * <p>策略：在给定提供商下查找匹配类型模型；若无，则全局查找匹配类型模型</p>
     *
     * @param providerId 提供商ID
     * @param modelType  模型类型
     * @return 模型配置
     */
    public ModelConfigDO resolveModelConfig(Long providerId, ModelTypeEnum modelType) {
        List<ModelConfigDO> providerModels = modelAppService.findModelsByProviderAndType(providerId, modelType);
        if (!providerModels.isEmpty()) {
            ModelConfigDO config = providerModels.get(0);
            log.debug("在提供商 [{}] 下找到 [{}] 类型模型: {}", providerId, modelType, config.getModelName());
            return config;
        }
        log.info("提供商 [{}] 下未找到 [{}] 类型模型，全局搜索", providerId, modelType);
        List<ModelConfigDO> allModels = modelAppService.findModelsByType(modelType);
        if (allModels.isEmpty()) {
            log.error("系统中未找到任何 [{}] 类型的模型配置", modelType);
            throw new BaseException(ErrorCodeEnum.NO_MODEL_CONFIG_AVAILABLE);
        }
        log.info("使用备用 [{}] 类型模型: {}", modelType, allModels.get(0).getModelName());
        return allModels.get(0);
    }

    /**
     * 根据模型配置ID获取模型配置
     *
     * @param modelId 模型配置ID
     * @return 模型配置
     */
    public ModelConfigDO getModelConfigById(Long modelId) {
        ModelConfigDO config = modelConfigMapper.selectById(modelId);
        if (config == null) {
            log.error("模型配置不存在: modelId={}", modelId);
            throw new BaseException(ErrorCodeEnum.MODEL_NOT_EXIST);
        }
        return config;
    }

    /**
     * 根据提供商ID获取模型提供商
     *
     * @param providerId 提供商ID
     * @return 模型提供商
     */
    public ModelProviderDO getModelProviderById(Long providerId) {
        ModelProviderDO provider = providerMapper.selectById(providerId);
        if (provider == null) {
            log.error("模型提供商不存在: providerId={}", providerId);
            throw new BaseException(ErrorCodeEnum.PROVIDER_NOT_EXIST);
        }
        return provider;
    }
}