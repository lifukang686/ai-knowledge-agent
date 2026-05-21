package com.fukang.knowledge.agent.rag;

import com.fukang.knowledge.agent.api.qa.dto.QaResp;
import com.fukang.knowledge.agent.common.enums.ErrorCodeEnum;
import com.fukang.knowledge.agent.common.exception.BaseException;
import com.fukang.knowledge.agent.infrastructure.persistence.mapper.KnowledgeBaseMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * RAG 问答编排服务
 * <p>负责 RAG 问答流程的完整编排，包括知识库校验、查询改写、语义检索（后续迭代）和答案生成（后续迭代）。
 * 当前阶段完成知识库校验和查询改写，返回占位回答作为后续功能的接入点</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RagAppService {

    private final QueryRewriteService queryRewriteService;
    private final KnowledgeBaseMapper knowledgeBaseMapper;

    /**
     * RAG 问答核心流程
     * <p>校验知识库存在性 → 调用查询改写 → 返回回答（当前为占位）。
     * 后续迭代将在此接入语义检索和 LLM 答案生成</p>
     *
     * @param question        用户自然语言问题
     * @param knowledgeBaseId 目标知识库 ID
     * @param conversationId  会话 ID（可选，预留多轮对话）
     * @return 问答响应，包含回答文本和改写后的查询
     * @throws BaseException 知识库不存在时抛出 KNOWLEDGE_BASE_NOT_EXIST
     */
    public QaResp answer(String question, Long knowledgeBaseId, Long conversationId) {
        if (knowledgeBaseMapper.selectById(knowledgeBaseId) == null) {
            log.warn("知识库不存在: id={}", knowledgeBaseId);
            throw new BaseException(ErrorCodeEnum.KNOWLEDGE_BASE_NOT_EXIST);
        }

        String rewrittenQuery = queryRewriteService.rewrite(question);

        String answer = "RAG 检索和答案生成功能将在后续迭代中实现。改写后的查询：" + rewrittenQuery;

        return new QaResp(answer, rewrittenQuery, "success");
    }
}