package com.fukang.knowledge.agent.api.model;

import com.fukang.knowledge.agent.api.model.dto.ModelConfigReq;
import com.fukang.knowledge.agent.api.model.dto.ModelConfigUpdateReq;
import com.fukang.knowledge.agent.api.model.dto.ProviderReq;
import com.fukang.knowledge.agent.api.model.dto.ProviderResp;
import com.fukang.knowledge.agent.api.model.dto.ProviderUpdateReq;
import com.fukang.knowledge.agent.application.model.command.ModelConfigCommand;
import com.fukang.knowledge.agent.application.model.command.ModelConfigUpdateCommand;
import com.fukang.knowledge.agent.application.model.command.ProviderCommand;
import com.fukang.knowledge.agent.application.model.command.ProviderUpdateCommand;
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
 * @author lfk68
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
        return Result.success(modelAppService.createProvider(
                new ProviderCommand(req.name(), req.apiBaseUrl(), req.apiKey(), req.description())));
    }

    /**
     * 查询所有模型提供商列表
     *
     * @return 提供商列表
     */
    @GetMapping("/providers")
    public Result<List<ProviderResp>> listProviders() {
        return Result.success(modelAppService.listProviders().stream().map(this::toProviderResp).toList());
    }

    /**
     * 删除模型提供商
     * <p>根据ID删除模型提供商，同时级联删除该提供商下的所有模型配置数据</p>
     *
     * @param id 模型提供商ID
     * @return 空成功响应
     */
    @DeleteMapping("/providers/{id}")
    public Result<Void> deleteProvider(@PathVariable("id") Long id) {
        modelAppService.deleteProvider(id);
        return Result.success();
    }

    /**
     * 更新模型提供商
     * <p>根据ID更新模型提供商的字段，仅更新请求中非空的字段</p>
     *
     * @param id  模型提供商ID
     * @param req 模型提供商更新请求参数
     * @return 空成功响应
     */
    @PutMapping("/providers/{id}")
    public Result<Void> updateProvider(@PathVariable("id") Long id, @RequestBody @Validated ProviderUpdateReq req) {
        modelAppService.updateProvider(id,
                new ProviderUpdateCommand(req.name(), req.apiBaseUrl(), req.apiKey(), req.description()));
        return Result.success();
    }

    /**
     * 创建模型配置
     * @param req 模型配置创建请求参数
     * @return 新创建的模型配置ID
     */
    @PostMapping("/configs")
    public Result<Long> createModelConfig(@RequestBody @Validated ModelConfigReq req) {
        return Result.success(modelAppService.createModelConfig(
                new ModelConfigCommand(req.providerId(), req.modelName(), req.modelType(), req.defaultParams())));
    }

    /**
     * 删除模型配置
     * <p>根据ID物理删除模型配置，不支持批量删除</p>
     *
     * @param id 模型配置ID
     * @return 空成功响应
     */
    @DeleteMapping("/configs/{id}")
    public Result<Void> deleteModelConfig(@PathVariable("id") Long id) {
        modelAppService.deleteModelConfig(id);
        return Result.success();
    }

    /**
     * 更新模型配置
     * <p>根据ID更新模型配置的字段，仅更新请求中非空的字段</p>
     *
     * @param req 模型配置更新请求参数
     * @return 空成功响应
     */
    @PutMapping("/configs/{id}")
    public Result<Void> updateModelConfig(@PathVariable("id") Long id, @RequestBody @Validated ModelConfigUpdateReq req) {
        modelAppService.updateModelConfig(id,
                new ModelConfigUpdateCommand(req.providerId(), req.modelName(), req.modelType(), req.defaultParams()));
        return Result.success();
    }

    /**
     * 根据提供商ID查询模型配置列表
     *
     * @param providerId 提供商ID
     * @return 该提供商下的模型配置列表
     */
    @GetMapping("/configs")
    public Result<List<ModelConfigDO>> listModelConfigs(@RequestParam("providerId") String providerId) {
        return Result.success(modelAppService.listModelConfigs(Long.valueOf(providerId)));
    }

    /**
     * 设置默认模型提供商
     * <p>系统中只能存在一个默认提供商，设置新的默认提供商会自动取消旧默认</p>
     *
     * @param id 模型提供商ID
     * @return 空成功响应
     */
    @PutMapping("/providers/{id}/default")
    public Result<Void> setDefaultProvider(@PathVariable("id") Long id) {
        modelAppService.setDefaultProvider(id);
        return Result.success();
    }

    /**
     * 取消默认模型提供商
     *
     * @param id 模型提供商ID
     * @return 空成功响应
     */
    @DeleteMapping("/providers/{id}/default")
    public Result<Void> cancelDefaultProvider(@PathVariable("id") Long id) {
        modelAppService.cancelDefaultProvider(id);
        return Result.success();
    }

    /**
     * 转换提供商响应，避免明文 API Key 返回前端。
     */
    private ProviderResp toProviderResp(ModelProviderDO provider) {
        return new ProviderResp(provider.getId(), provider.getName(), provider.getApiBaseUrl(),
                maskApiKey(provider.getApiKey()), provider.getDescription(), provider.getIsDefault(),
                provider.getCreateTime(), provider.getUpdateTime());
    }

    /**
     * 脱敏 API Key。
     */
    private String maskApiKey(String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return "";
        }
        if (apiKey.length() <= 8) {
            return "****";
        }
        return apiKey.substring(0, 4) + "****" + apiKey.substring(apiKey.length() - 4);
    }
}
