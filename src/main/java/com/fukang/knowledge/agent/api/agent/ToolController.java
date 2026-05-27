package com.fukang.knowledge.agent.api.agent;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.fukang.knowledge.agent.api.agent.dto.ToolCreateReq;
import com.fukang.knowledge.agent.api.agent.dto.ToolResp;
import com.fukang.knowledge.agent.api.agent.dto.ToolUpdateReq;
import com.fukang.knowledge.agent.agent.registry.ToolRegistry;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.PageResponse;
import com.fukang.knowledge.agent.common.result.Result;
import com.fukang.knowledge.agent.domain.agent.model.ToolDefinition;
import com.fukang.knowledge.agent.domain.agent.model.ToolInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 工具管理控制器
 * <p>提供工具定义的注册和查询接口</p>
 */
@RestController
@RequestMapping("/api/tools")
@RequiredArgsConstructor
public class ToolController {

    private final ToolRegistry toolRegistry;

    /**
     * 注册工具
     *
     * @param req 工具定义信息
     * @return 注册结果
     */
    @PostMapping
    public Result<ToolResp> register(@RequestBody ToolCreateReq req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "工具名称不能为空");
        }
        if (req.executorConfig() == null || req.executorConfig().isBlank()) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "执行器配置不能为空");
        }

        ToolDefinition definition = new ToolDefinition(null, req.name(),
                req.description(), req.executorType(), req.executorConfig(),
                req.parametersSchema(), true);
        toolRegistry.register(definition);

        ToolResp resp = new ToolResp(null, req.name(), req.description(),
                req.executorType(), req.executorConfig(),
                req.parametersSchema(), true);
        return Result.success(resp);
    }

    /**
     * 修改工具信息
     * <p>根据工具 ID 更新工具定义，需要校验必填字段</p>
     *
     * @param id  工具 ID
     * @param req 工具更新信息
     * @return 更新后的工具信息
     */
    @PutMapping("/{id}")
    public Result<ToolResp> update(@PathVariable(name = "id") String id, @RequestBody ToolUpdateReq req) {
        if (req.name() == null || req.name().isBlank()) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "工具名称不能为空");
        }
        if (req.executorConfig() == null || req.executorConfig().isBlank()) {
            throw new BaseException(ErrorCodeEnum.BAD_REQUEST.getCode(), "执行器配置不能为空");
        }

        ToolDefinition definition = new ToolDefinition(Long.valueOf(id), req.name(),
                req.description(), req.executorType(), req.executorConfig(),
                req.parametersSchema(), true);
        ToolResp resp = toolRegistry.update(Long.valueOf(id), definition);
        return Result.success(resp);
    }

    /**
     * 删除工具
     * <p>根据工具 ID 删除工具定义，不存在则抛出异常</p>
     *
     * @param id 工具 ID
     * @return 删除结果
     */
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable(name = "id") Long id) {
        toolRegistry.deleteById(id);
        return Result.success();
    }

    /**
     * 获取所有已注册工具列表
     *
     * @return 可用工具信息列表
     */
    @GetMapping
    public Result<List<ToolInfo>> list() {
        List<ToolInfo> tools = toolRegistry.listAvailableTools();
        return Result.success(tools);
    }

    /**
     * 分页查询工具列表
     * <p>支持关键字模糊搜索工具名称和描述，按创建时间倒序排列。
     * 分页参数通过 MyBatis-Plus Page 对象自动绑定</p>
     *
     * @param pageQuery 分页查询对象，从请求参数 current(页码)/size(每页条数) 自动绑定
     * @param keyword   搜索关键字，可选
     * @return 分页响应，包含工具信息列表和分页信息
     */
    @GetMapping("/page")
    public Result<PageResponse<ToolResp>> listPage(
            Page<?> pageQuery,
            @RequestParam(value = "keyword", required = false) String keyword) {
        return Result.success(toolRegistry.listTools(pageQuery, keyword));
    }
}