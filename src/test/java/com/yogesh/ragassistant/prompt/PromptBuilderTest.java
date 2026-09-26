package com.yogesh.ragassistant.prompt;

import com.yogesh.ragassistant.retriever.RetrievedChunk;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PromptBuilderTest {

    private PromptBuilder promptBuilder;

    @BeforeEach
    void setUp() {
        promptBuilder = new PromptBuilder();
    }

    @Test
    @DisplayName("Should build grounded system prompt containing chunk details and strict instructions")
    void testBuildSystemPrompt() {
        RetrievedChunk chunk1 = RetrievedChunk.builder()
                .chunkId(UUID.randomUUID())
                .documentTitle("SecurityPolicy.pdf")
                .pageNumber(4)
                .chunkIndex(2)
                .chunkText("All production API tokens must rotate every 90 days.")
                .hybridScore(0.88)
                .build();

        String systemPrompt = promptBuilder.buildSystemPrompt(List.of(chunk1));

        assertNotNull(systemPrompt);
        assertTrue(systemPrompt.contains("SecurityPolicy.pdf"));
        assertTrue(systemPrompt.contains("Page Number: 4"));
        assertTrue(systemPrompt.contains("All production API tokens must rotate every 90 days."));
        assertTrue(systemPrompt.contains("I cannot answer this question based on the provided documents"));
    }

    @Test
    @DisplayName("Should handle empty chunks with fallback context")
    void testEmptyChunksPrompt() {
        String systemPrompt = promptBuilder.buildSystemPrompt(List.of());
        assertTrue(systemPrompt.contains("No relevant documents found."));
    }
}
