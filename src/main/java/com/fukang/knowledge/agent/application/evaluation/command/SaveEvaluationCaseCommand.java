package com.fukang.knowledge.agent.application.evaluation.command;

import java.util.List;

/**
 * 保存评测用例命令。
 */
public record SaveEvaluationCaseCommand(
        String question,
        String expectedAnswer,
        List<String> expectedKeywords,
        List<Long> expectedChunkIds,
        String expectedStatus,
        String metadata,
        Boolean enabled
) {
}
