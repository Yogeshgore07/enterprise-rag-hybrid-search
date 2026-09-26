package com.yogesh.ragassistant.retriever;

import com.yogesh.ragassistant.config.RagProperties;
import com.yogesh.ragassistant.repository.DocumentChunkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HybridRetrieverTest {

    @Mock
    private DocumentChunkRepository chunkRepository;

    private RagProperties ragProperties;
    private HybridRetriever hybridRetriever;

    @BeforeEach
    void setUp() {
        ragProperties = new RagProperties();
        ragProperties.getRetrieval().setTopK(10);
        ragProperties.getRetrieval().setVectorWeight(0.6);
        ragProperties.getRetrieval().setKeywordWeight(0.4);
        ragProperties.getRetrieval().setMinScoreThreshold(0.0);

        hybridRetriever = new HybridRetriever(chunkRepository, ragProperties);
    }

    @Test
    @DisplayName("Should successfully merge semantic and keyword results and calculate weighted scores")
    void testHybridRetrievalAndScoring() {
        UUID docId = UUID.randomUUID();
        UUID chunk1Id = UUID.randomUUID();
        UUID chunk2Id = UUID.randomUUID();
        UUID chunk3Id = UUID.randomUUID();

        // Chunk 1 is found in both semantic and keyword search
        DocumentChunkRepository.ChunkSearchResult sem1 = createMockResult(chunk1Id, docId, "Doc A", 1, 0, "Chunk 1 text", 0.95);
        DocumentChunkRepository.ChunkSearchResult sem2 = createMockResult(chunk2Id, docId, "Doc A", 1, 1, "Chunk 2 text", 0.80);

        DocumentChunkRepository.ChunkSearchResult key1 = createMockResult(chunk1Id, docId, "Doc A", 1, 0, "Chunk 1 text", 0.50);
        DocumentChunkRepository.ChunkSearchResult key3 = createMockResult(chunk3Id, docId, "Doc A", 2, 2, "Chunk 3 text", 0.30);

        when(chunkRepository.searchSemantic(anyString(), any(), anyInt())).thenReturn(List.of(sem1, sem2));
        when(chunkRepository.searchKeyword(anyString(), any(), anyInt())).thenReturn(List.of(key1, key3));

        float[] mockVector = new float[1536];
        List<RetrievedChunk> results = hybridRetriever.retrieve("What is the architecture?", mockVector, null, null, null, null);

        assertNotNull(results);
        assertEquals(3, results.size(), "Deduplicated candidates should be 3");

        // Chunk 1 should be ranked #1 because it has high scores in both semantic and keyword
        RetrievedChunk topChunk = results.get(0);
        assertEquals(chunk1Id, topChunk.getChunkId());
        assertTrue(topChunk.getHybridScore() > 0.0);
        assertNotNull(topChunk.getSnippet());
    }

    private DocumentChunkRepository.ChunkSearchResult createMockResult(
            UUID id, UUID docId, String title, int page, int index, String text, double score) {
        return new DocumentChunkRepository.ChunkSearchResult() {
            @Override public UUID getId() { return id; }
            @Override public UUID getDocumentId() { return docId; }
            @Override public String getDocumentTitle() { return title; }
            @Override public Integer getPageNumber() { return page; }
            @Override public Integer getChunkIndex() { return index; }
            @Override public String getChunkText() { return text; }
            @Override public Double getScore() { return score; }
        };
    }
}
