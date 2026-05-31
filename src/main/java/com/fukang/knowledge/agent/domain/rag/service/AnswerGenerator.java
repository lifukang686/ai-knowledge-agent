package com.fukang.knowledge.agent.domain.rag.service;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;

import java.util.List;

public interface AnswerGenerator {
    String generateAnswer(List<SearchResult> results, String query);

    String generateAnswer(List<SearchResult> results, String query, String conversationMemory);
}
