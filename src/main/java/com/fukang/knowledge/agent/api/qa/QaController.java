package com.fukang.knowledge.agent.api.qa;

import com.fukang.knowledge.agent.api.qa.dto.QaReq;
import com.fukang.knowledge.agent.api.qa.dto.QaResp;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.common.result.Result;
import com.fukang.knowledge.agent.application.rag.RagAppService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * QA 问答控制器
 * <p>提供基于 RAG 的知识库智能问答接口，支持多轮对话上下文关联</p>
 */
@RestController
@RequestMapping("/api/qa")
@RequiredArgsConstructor
public class QaController {

    private final RagAppService ragAppService;

    /**
     * 知识库智能问答
     * <p>接收用户自然语言问题，经过查询改写后在指定知识库中检索相关内容，
     * 结合大语言模型生成回答。支持通过 conversationId 关联多轮对话上下文。
     *
     * @param req 问答请求参数，question 必填
     * @return 问答响应，包含回答文本和改写后的查询
     */
    @PostMapping
    public Result<QaResp> ask(@RequestBody QaReq req) {
        if (req.question() == null || req.question().isBlank()) {
            throw new BaseException(ErrorCodeEnum.QUESTION_EMPTY);
        }

        QaResp qaResp = ragAppService.answer(req.question(), req.knowledgeBaseId(), req.conversationId());
        return Result.success(qaResp);
    }
}