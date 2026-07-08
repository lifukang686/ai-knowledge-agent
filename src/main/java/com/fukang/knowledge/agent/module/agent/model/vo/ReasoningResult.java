package com.fukang.knowledge.agent.module.agent.model.vo;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 推理结果值对象
 * <p>由 Reasoner 调用 LLM 生成，决定 Agent 下一步应该执行的动作</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReasoningResult {
    /** 决策类型 */
    private Decision decision;

    /** 决策内容（继续时为新参数、完成时为最终答案、重试时为原因、终止时为错误说明） */
    private String content;

    /**
     * Agent 决策类型枚举
     */
    public enum Decision {
        /** 继续执行下一个计划步骤 */
        CONTINUE,
        /** 已可给出最终答案 */
        FINAL_ANSWER,
        /** 重试最近一次工具调用 */
        RETRY,
        /** 终止当前任务 */
        ABORT
    }

}
