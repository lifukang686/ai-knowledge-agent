package com.fukang.knowledge.agent.infrastructure.tool;

import java.util.Map;

/**
 * 本地方法工具统一接口。
 * <p>内部业务工具实现该接口后，可通过 LOCAL_METHOD 执行器被 Plan-Execute Agent 安全调用。</p>
 */
public interface LocalMethodTool {

    /** 工具名称，必须与 ToolDefinition.name 保持一致。 */
    String name();

    /** 执行业务工具，参数来自 LLM 规划结果。 */
    Object execute(Map<String, Object> arguments);
}
