package com.fukang.knowledge.agent.module.model.controller;

import com.fukang.knowledge.agent.module.model.model.dto.ModelConfigReq;
import com.fukang.knowledge.agent.module.model.model.dto.ModelConfigUpdateReq;
import com.fukang.knowledge.agent.module.model.model.dto.ProviderReq;
import com.fukang.knowledge.agent.module.model.model.resp.ProviderResp;
import com.fukang.knowledge.agent.module.model.model.dto.ProviderUpdateReq;
import com.fukang.knowledge.agent.module.model.model.dto.ModelConfigCommand;
import com.fukang.knowledge.agent.module.model.model.dto.ModelConfigUpdateCommand;
import com.fukang.knowledge.agent.module.model.model.dto.ProviderCommand;
import com.fukang.knowledge.agent.module.model.model.dto.ProviderUpdateCommand;
import com.fukang.knowledge.agent.module.model.service.ModelService;
import com.fukang.knowledge.agent.common.result.Result;
import com.fukang.knowledge.agent.module.model.model.entity.ModelConfigEntity;
import com.fukang.knowledge.agent.module.model.model.entity.ModelProviderEntity;
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

    private final ModelService modelService;

    /**
     * 创建模型提供商
     *
     * @param req 提供商创建请求参数
     * @return 新创建的提供商ID
     */
    @PostMapping("/providers")
    public Result<Long> createProvider(@RequestBody @Validated ProviderReq req) {
        return Result.success(modelService.createProvider(
                new ProviderCommand(req.getName(), req.getApiBaseUrl(), req.getApiKey(), req.getDescription())));
    }

    /**
     * 查询所有模型提供商列表
     *
     * @return 提供商列表
     */
    @GetMapping("/providers")
    public Result<List<ProviderResp>> listProviders() {
        return Result.success(modelService.listProviders().stream().map(this::toProviderResp).toList());
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
        modelService.deleteProvider(id);
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
        modelService.updateProvider(id,
                new ProviderUpdateCommand(req.getName(), req.getApiBaseUrl(), req.getApiKey(), req.getDescription()));
        return Result.success();
    }

    /**
     * 创建模型配置
     * @param req 模型配置创建请求参数
     * @return 新创建的模型配置ID
     */
    @PostMapping("/configs")
    public Result<Long> createModelConfig(@RequestBody @Validated ModelConfigReq req) {
        return Result.success(modelService.createModelConfig(
                new ModelConfigCommand(req.getProviderId(), req.getModelName(), req.getModelType(), req.getDefaultParams())));
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
        modelService.deleteModelConfig(id);
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
        modelService.updateModelConfig(id,
                new ModelConfigUpdateCommand(req.getProviderId(), req.getModelName(), req.getModelType(), req.getDefaultParams()));
        return Result.success();
    }

    /**
     * 根据提供商ID查询模型配置列表
     *
     * @param providerId 提供商ID
     * @return 该提供商下的模型配置列表
     */
    @GetMapping("/configs")
    public Result<List<ModelConfigEntity>> listModelConfigs(@RequestParam("providerId") String providerId) {
        return Result.success(modelService.listModelConfigs(Long.valueOf(providerId)));
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
        modelService.setDefaultProvider(id);
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
        modelService.cancelDefaultProvider(id);
        return Result.success();
    }

    /**
     * 转换提供商响应，避免明文 API Key 返回前端。
     */
    private ProviderResp toProviderResp(ModelProviderEntity provider) {
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
