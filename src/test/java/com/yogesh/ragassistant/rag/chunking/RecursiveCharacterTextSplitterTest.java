package com.yogesh.ragassistant.rag.chunking;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class RecursiveCharacterTextSplitterTest {

    private RecursiveCharacterTextSplitter textSplitter;

    @BeforeEach
    void setUp() {
        // Chunk size: 50 tokens (~200 chars), overlap: 10 tokens (~40 chars)
        textSplitter = new RecursiveCharacterTextSplitter(50, 10, List.of("\n\n", "\n", ". ", " ", ""));
    }

    @Test
    @DisplayName("Should split long text into multiple chunks respecting token boundaries")
    void testSplitLongText() {
        String paragraph1 = "Enterprise knowledge assistants require scalable search architectures. " +
                "Modern pipelines combine vector embeddings with lexical indexing for high recall.";
        String paragraph2 = "Recursive character text splitting preserves natural sentence boundaries. " +
                "Paragraph breaks and sentence periods are prioritized over hard character cuts.";
        String paragraph3 = "PostgreSQL pgvector provides vector indexing capabilities such as HNSW and IVFFlat. " +
                "Full-text search provides exact term matching with tsvector and ts_rank_cd.";

        String fullText = paragraph1 + "\n\n" + paragraph2 + "\n\n" + paragraph3;

        List<ChunkWithMetadata> chunks = textSplitter.splitTextWithMetadata(fullText, "TestDoc.pdf", 1, 0);

        assertNotNull(chunks);
        assertFalse(chunks.isEmpty());
        assertTrue(chunks.size() >= 2);

        for (int i = 0; i < chunks.size(); i++) {
            ChunkWithMetadata chunk = chunks.get(i);
            assertEquals(i, chunk.getChunkIndex());
            assertEquals(1, chunk.getPageNumber());
            assertEquals("TestDoc.pdf", chunk.getDocumentTitle());
            assertFalse(chunk.getText().isBlank());
            assertTrue(chunk.getTokenCount() > 0);
        }
    }

    @Test
    @DisplayName("Should detect headings in text chunks")
    void testHeadingDetection() {
        String markdownWithHeading = "# System Architecture\n\n" +
                "The system uses Spring Boot 3 with Java 21, pgvector for semantic retrieval, " +
                "and PostgreSQL tsvector for keyword ranking.";

        List<ChunkWithMetadata> chunks = textSplitter.splitTextWithMetadata(markdownWithHeading, "Arch.md", 1, 0);

        assertFalse(chunks.isEmpty());
        assertEquals("System Architecture", chunks.get(0).getHeading());
    }

    @Test
    @DisplayName("Should handle empty and blank text gracefully")
    void testEmptyText() {
        List<ChunkWithMetadata> emptyChunks = textSplitter.splitTextWithMetadata("", "Empty.txt", 1, 0);
        assertTrue(emptyChunks.isEmpty());

        List<ChunkWithMetadata> nullChunks = textSplitter.splitTextWithMetadata(null, "Null.txt", 1, 0);
        assertTrue(nullChunks.isEmpty());
    }
}
