package com.fukang.knowledge.agent.infrastructure.chunk;

import com.fukang.knowledge.agent.domain.knowledge.model.ChunkResult;
import com.fukang.knowledge.agent.domain.knowledge.model.DocumentParseResult;
import com.fukang.knowledge.agent.infrastructure.config.ChunkingProperties;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class Langchain4jChunkStrategyTest {

    @Test
    void chunkExtractsStructureMetadataAndRemovesInternalMarkersFromDisplayText() {
        ChunkingProperties properties = new ChunkingProperties();
        properties.setStrategy("paragraph");
        properties.setMaxSegmentSize(500);
        properties.setOverlapSize(0);
        Langchain4jChunkStrategy strategy = new Langchain4jChunkStrategy(properties);

        DocumentParseResult parseResult = new DocumentParseResult(
                "[[PAGE:2]]\n[[SECTION:安装说明]]\n安装步骤如下。\n[[TABLE]]\n参数 | 说明\n[[/TABLE]]",
                "manual.docx",
                "docx",
                Map.of(),
                LocalDateTime.now());

        ChunkResult result = strategy.chunk(parseResult);

        assertThat(result.chunks()).hasSize(1);
        assertThat(result.chunks().getFirst().metadata()).containsEntry("pageNumber", "2");
        assertThat(result.chunks().getFirst().metadata()).containsEntry("sectionTitle", "安装说明");
        assertThat(result.chunks().getFirst().chunkText())
                .doesNotContain("[[PAGE:")
                .doesNotContain("[[SECTION:")
                .contains("安装步骤如下。")
                .contains("参数 | 说明");
    }
}
