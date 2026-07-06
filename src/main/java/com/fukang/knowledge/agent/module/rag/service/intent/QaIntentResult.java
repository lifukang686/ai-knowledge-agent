package com.fukang.knowledge.agent.module.rag.service.intent;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * QA 意图识别结果。
 *
 * @param intent     意图分类
 * @param confidence 置信度，0-1
 * @param reason     简短原因，便于排查路由行为
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QaIntentResult {
    private QaIntent intent;

    private double confidence;

    private String reason;

    /**
     * 创建意图识别结果。
     */
    public static QaIntentResult of(QaIntent intent, double confidence, String reason) {
        return new QaIntentResult(intent, confidence, reason);
    }

}
