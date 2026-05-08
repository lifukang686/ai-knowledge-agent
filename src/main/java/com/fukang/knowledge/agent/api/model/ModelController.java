package com.fukang.knowledge.agent.api.model;

import com.fukang.knowledge.agent.api.model.dto.ModelConfigReq;
import com.fukang.knowledge.agent.api.model.dto.ProviderReq;
import com.fukang.knowledge.agent.application.model.ModelAppService;
import com.fukang.knowledge.agent.common.result.Result;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelConfigDO;
import com.fukang.knowledge.agent.infrastructure.persistence.entity.ModelProviderDO;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 模型管理控制器
 * <p>提供 AI 模型提供商和模型配置的增删查接口</p>
 */
@RestController
@RequestMapping("/api/models")
@RequiredArgsConstructor
public class ModelController {

    private final ModelAppService modelAppService;

    /**
     * 创建模型提供商
     *
     * @param req 提供商创建请求参数
     * @return 新创建的提供商ID
     */
    @PostMapping("/providers")
    public Result<Long> createProvider(@RequestBody @Validated ProviderReq req) {
        return Result.success(modelAppService.createProvider(req));
    }

    /**
     * 查询所有模型提供商列表
     *
     * @return 提供商列表
     */
    @GetMapping("/providers")
    public Result<List<ModelProviderDO>> listProviders() {
        return Result.success(modelAppService.listProviders());
    }

    /**
     * 创建模型配置
     *
     * @param req 模型配置创建请求参数
     * @return 新创建的模型配置ID
     */
    @PostMapping("/configs")
    public Result<Long> createModelConfig(@RequestBody @Validated ModelConfigReq req) {
        return Result.success(modelAppService.createModelConfig(req));
    }

    /**
     * 根据提供商ID查询模型配置列表
     *
     * @param providerId 提供商ID
     * @return 该提供商下的模型配置列表
     */
    @GetMapping("/configs")
    public Result<List<ModelConfigDO>> listModelConfigs(@RequestParam Long providerId) {
        return Result.success(modelAppService.listModelConfigs(providerId));
    }
}
