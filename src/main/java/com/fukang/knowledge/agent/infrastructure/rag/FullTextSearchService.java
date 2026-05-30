package com.fukang.knowledge.agent.infrastructure.rag;

import com.fukang.knowledge.agent.domain.rag.model.SearchResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Types;
import java.util.List;

/**
 * PostgreSQL 全文检索服务
 * <p>基于 PostgreSQL tsvector/ts_rank 实现 BM25 类似的全文检索能力，
 * 通过 GIN 索引加速。查询时 JOIN document 表实现知识库级别过滤</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FullTextSearchService {

    private static final String SEARCH_SQL = """
            SELECT c.id AS chunk_id, c.chunk_text,
                   ts_rank(to_tsvector('simple', c.chunk_text), plainto_tsquery('simple', ?)) AS score
            FROM document_chunk c
            JOIN document d ON d.id = c.document_id
            WHERE to_tsvector('simple', c.chunk_text) @@ plainto_tsquery('simple', ?)
              AND (?::bigint IS NULL OR d.knowledge_base_id = ?)
            ORDER BY score DESC
            LIMIT ?
            """;

    private final JdbcTemplate jdbcTemplate;

    /**
     * 执行 PostgreSQL 全文检索
     *
     * @param queryText       查询文本
     * @param knowledgeBaseId 知识库 ID，null 表示全量检索
     * @param topK            最大返回结果数
     * @param minScore        ts_rank 最低分数阈值
     * @return 按 ts_rank 降序排列的检索结果
     */
    public List<SearchResult> search(String queryText, Long knowledgeBaseId, int topK, double minScore) {
        long start = System.currentTimeMillis();

        List<SearchResult> results = jdbcTemplate.query(
                SEARCH_SQL,
                ps -> {
                    ps.setString(1, queryText);
                    ps.setString(2, queryText);
                    if (knowledgeBaseId != null) {
                        ps.setLong(3, knowledgeBaseId);
                        ps.setLong(4, knowledgeBaseId);
                    } else {
                        ps.setNull(3, Types.BIGINT);
                        ps.setNull(4, Types.BIGINT);
                    }
                    ps.setInt(5, topK);
                },
                (rs, rowNum) -> {
                    double score = rs.getDouble("score");
                    if (score < minScore) {
                        return null;
                    }
                    return new SearchResult(
                            rs.getLong("chunk_id"),
                            rs.getString("chunk_text"),
                            score,
                            null);
                }
        );

        // 过滤掉因 minScore 被标记为 null 的行
        results.removeIf(r -> r == null);

        log.info("全文检索完成: query=[{}], knowledgeBaseId=[{}], resultCount=[{}], elapsedMs=[{}]",
                queryText, knowledgeBaseId, results.size(), System.currentTimeMillis() - start);
        return results;
    }
}