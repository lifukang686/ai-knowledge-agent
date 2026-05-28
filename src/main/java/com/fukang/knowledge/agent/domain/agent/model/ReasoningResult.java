package com.fukang.knowledge.agent.domain.agent.model;

/**
 * 推理结果值对象
 * <p>由 Reasoner 调用 LLM 生成，决定 Agent 下一步应该执行的动作</p>
 *
 * @param decision 决策类型
 * @param content  决策内容（继续时为新参数、完成时为最终答案、重试时为原因、终止时为错误说明）
 */
public record ReasoningResult(
    Decision decision,
    String content
) {

    /**
     * Agent 决策类型枚举
     */
    public enum Decision {
        /** 继续执行下一个计划步骤 */
        CONTINUE,
        /** 任务完成，content 为最终答案 */
        FINAL_ANSWER,
        /** 重试当前步骤 */
        RETRY,
        /** 终止执行 */
        ABORT
    }
}