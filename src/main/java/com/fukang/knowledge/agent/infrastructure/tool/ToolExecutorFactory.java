package com.fukang.knowledge.agent.infrastructure.tool;

import com.fukang.knowledge.agent.common.enums.ExecutorTypeEnum;
import com.fukang.knowledge.agent.domain.agent.service.ToolExecutor;
import org.springframework.stereotype.Component;

/**
 * 工具执行器工厂
 * <p>根据执行器类型返回对应的 ToolExecutor 实例。
 * 使用策略模式，将执行器选择逻辑集中管理</p>
 */
@Component
public class ToolExecutorFactory {

    private final HttpToolExecutor httpExecutor;
    private final SqlToolExecutor sqlExecutor;

    public ToolExecutorFactory(HttpToolExecutor httpExecutor, SqlToolExecutor sqlExecutor) {
        this.httpExecutor = httpExecutor;
        this.sqlExecutor = sqlExecutor;
    }

    /**
     * 根据执行器类型获取对应的执行器实例
     *
     * @param executorType 执行器类型
     * @return 对应的 ToolExecutor 实例
     * @throws IllegalArgumentException 如果类型不支持
     */
    public ToolExecutor getExecutor(ExecutorTypeEnum executorType) {
        if (executorType == null) {
            throw new IllegalArgumentException("执行器类型不能为空");
        }
        return switch (executorType) {
            case HTTP -> httpExecutor;
            case SQL -> sqlExecutor;
            case LOCAL_METHOD ->
                    throw new UnsupportedOperationException("本地方法执行器待后续版本实现");
        };
    }
}