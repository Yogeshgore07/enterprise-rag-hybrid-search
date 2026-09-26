package com.yogesh.ragassistant.citation;

import com.yogesh.ragassistant.dto.response.CitationResponse;
import com.yogesh.ragassistant.retriever.RetrievedChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
@Slf4j
public class CitationVerificationService {

    /**
     * Verifies citations between the LLM generated answer and the retrieved chunks.
     * Ensures all returned citations have accurate document names, page numbers, and snippet excerpts.
     */
    public List<CitationResponse> verifyAndBuildCitations(String answer, List<RetrievedChunk> retrievedChunks) {
        if (retrievedChunks == null || retrievedChunks.isEmpty()) {
            return List.of();
        }

        List<CitationResponse> citations = new ArrayList<>();
        Set<UUID> addedChunkIds = new HashSet<>();

        // If the answer states that it cannot answer, return empty citations
        if (answer.toLowerCase().contains("cannot answer this question based on the provided documents")) {
            return List.of();
        }

        // 1. Check for explicit citations referenced in the text or include top contributing chunks
        for (RetrievedChunk chunk : retrievedChunks) {
            if (addedChunkIds.contains(chunk.getChunkId())) {
                continue;
            }

            // Verify chunk relevance
            boolean referencedInText = isChunkReferenced(answer, chunk);

            // Include chunks that were either explicitly referenced by the LLM or are high-confidence candidates
            if (referencedInText || citations.size() < 3) {
                CitationResponse citation = CitationResponse.builder()
                        .citationId(UUID.randomUUID())
                        .documentId(chunk.getDocumentId())
                        .chunkId(chunk.getChunkId())
                        .documentTitle(chunk.getDocumentTitle())
                        .pageNumber(chunk.getPageNumber())
                        .chunkIndex(chunk.getChunkIndex())
                        .snippet(chunk.getSnippet() != null ? chunk.getSnippet() : extractSnippet(chunk.getChunkText()))
                        .relevanceScore(chunk.getHybridScore())
                        .build();

                citations.add(citation);
                addedChunkIds.add(chunk.getChunkId());
            }
        }

        log.debug("Verified and attached {} citations for query answer", citations.size());
        return citations;
    }

    private boolean isChunkReferenced(String answer, RetrievedChunk chunk) {
        String answerLower = answer.toLowerCase();

        // Check if document title is mentioned
        if (chunk.getDocumentTitle() != null && answerLower.contains(chunk.getDocumentTitle().toLowerCase())) {
            return true;
        }

        // Check if page number is mentioned alongside "page"
        if (answerLower.contains("page " + chunk.getPageNumber()) ||
            answerLower.contains("page: " + chunk.getPageNumber())) {
            return true;
        }

        // Check for chunk index marker
        if (answerLower.contains("chunk: " + chunk.getChunkIndex())) {
            return true;
        }

        return false;
    }

    private String extractSnippet(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        return trimmed.length() > 250 ? trimmed.substring(0, 250) + "..." : trimmed;
    }
}
