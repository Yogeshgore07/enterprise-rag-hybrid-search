package com.yogesh.ragassistant.retriever;

import com.yogesh.ragassistant.config.RagProperties;
import com.yogesh.ragassistant.repository.DocumentChunkRepository;
import com.yogesh.ragassistant.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

/**
 * Hybrid Retriever combining pgvector Cosine Similarity and PostgreSQL Full-Text Search.
 * 
 * Pipeline:
 * 1. Executes Semantic Search (pgvector cosine ops)
 * 2. Executes Keyword Search (PostgreSQL ts_rank_cd)
 * 3. Applies Min-Max score normalization
 * 4. Computes weighted hybrid score = (vectorWeight * norm_semantic) + (keywordWeight * norm_keyword)
 * 5. Deduplicates chunks and sorts by hybrid score descending
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class HybridRetriever {

    private final DocumentChunkRepository chunkRepository;
    private final RagProperties ragProperties;

    @Transactional(readOnly = true)
    public List<RetrievedChunk> retrieve(String query,
                                         float[] queryEmbedding,
                                         UUID documentIdFilter,
                                         Integer topKOverride,
                                         Double vectorWeightOverride,
                                         Double keywordWeightOverride) {
        return retrieve(query, queryEmbedding, documentIdFilter, null, topKOverride, vectorWeightOverride, keywordWeightOverride);
    }

    @Transactional(readOnly = true)
    public List<RetrievedChunk> retrieve(String query,
                                         float[] queryEmbedding,
                                         UUID documentIdFilter,
                                         UUID userIdFilter,
                                         Integer topKOverride,
                                         Double vectorWeightOverride,
                                         Double keywordWeightOverride) {

        int topK = topKOverride != null ? topKOverride : ragProperties.getRetrieval().getTopK();
        double vectorWeight = vectorWeightOverride != null ? vectorWeightOverride : ragProperties.getRetrieval().getVectorWeight();
        double keywordWeight = keywordWeightOverride != null ? keywordWeightOverride : ragProperties.getRetrieval().getKeywordWeight();
        double minScoreThreshold = ragProperties.getRetrieval().getMinScoreThreshold();

        long startTime = System.currentTimeMillis();

        // 1. Semantic Search
        String vectorString = VectorUtils.toPgVectorString(queryEmbedding);
        List<DocumentChunkRepository.ChunkSearchResult> semanticResults = List.of();
        try {
            semanticResults = chunkRepository.searchSemantic(vectorString, documentIdFilter, userIdFilter, topK);
        } catch (Exception ex) {
            log.warn("Semantic search failed or returned no results: {}", ex.getMessage());
        }

        // 2. Keyword Search
        List<DocumentChunkRepository.ChunkSearchResult> keywordResults = List.of();
        try {
            keywordResults = chunkRepository.searchKeyword(query, documentIdFilter, userIdFilter, topK);
        } catch (Exception ex) {
            log.warn("Keyword search failed or returned no results: {}", ex.getMessage());
        }

        log.debug("Retrieved {} semantic candidates and {} keyword candidates in {} ms",
                semanticResults.size(), keywordResults.size(), (System.currentTimeMillis() - startTime));

        if (semanticResults.isEmpty() && keywordResults.isEmpty()) {
            return List.of();
        }

        // 3. Normalize Scores
        Map<UUID, Double> normalizedSemanticScores = normalizeScores(semanticResults);
        Map<UUID, Double> normalizedKeywordScores = normalizeScores(keywordResults);

        // 4. Merge and calculate Hybrid Score
        Map<UUID, RetrievedChunk> mergedMap = new HashMap<>();

        // Process semantic candidates
        for (DocumentChunkRepository.ChunkSearchResult res : semanticResults) {
            UUID chunkId = res.getId();
            double normScore = normalizedSemanticScores.getOrDefault(chunkId, 0.0);

            RetrievedChunk chunk = RetrievedChunk.builder()
                    .chunkId(chunkId)
                    .documentId(res.getDocumentId())
                    .documentTitle(res.getDocumentTitle())
                    .pageNumber(res.getPageNumber())
                    .chunkIndex(res.getChunkIndex())
                    .chunkText(res.getChunkText())
                    .semanticScore(res.getScore())
                    .keywordScore(0.0)
                    .hybridScore(normScore * vectorWeight)
                    .snippet(createSnippet(res.getChunkText()))
                    .build();

            mergedMap.put(chunkId, chunk);
        }

        // Process keyword candidates
        for (DocumentChunkRepository.ChunkSearchResult res : keywordResults) {
            UUID chunkId = res.getId();
            double normScore = normalizedKeywordScores.getOrDefault(chunkId, 0.0);

            if (mergedMap.containsKey(chunkId)) {
                // Chunk found in both search engines!
                RetrievedChunk existing = mergedMap.get(chunkId);
                existing.setKeywordScore(res.getScore());
                existing.setHybridScore(existing.getHybridScore() + (normScore * keywordWeight));
            } else {
                RetrievedChunk chunk = RetrievedChunk.builder()
                        .chunkId(chunkId)
                        .documentId(res.getDocumentId())
                        .documentTitle(res.getDocumentTitle())
                        .pageNumber(res.getPageNumber())
                        .chunkIndex(res.getChunkIndex())
                        .chunkText(res.getChunkText())
                        .semanticScore(0.0)
                        .keywordScore(res.getScore())
                        .hybridScore(normScore * keywordWeight)
                        .snippet(createSnippet(res.getChunkText()))
                        .build();

                mergedMap.put(chunkId, chunk);
            }
        }

        // 5. Filter and Sort descending
        List<RetrievedChunk> ranked = mergedMap.values().stream()
                .filter(c -> c.getHybridScore() >= minScoreThreshold)
                .sorted((a, b) -> Double.compare(b.getHybridScore(), a.getHybridScore()))
                .limit(topK)
                .toList();

        long totalDuration = System.currentTimeMillis() - startTime;
        log.info("Hybrid search returned {} ranked chunks for query '{}' in {} ms",
                ranked.size(), query, totalDuration);

        return ranked;
    }

    private Map<UUID, Double> normalizeScores(List<DocumentChunkRepository.ChunkSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return Map.of();
        }

        double min = Double.MAX_VALUE;
        double max = -Double.MAX_VALUE;

        for (DocumentChunkRepository.ChunkSearchResult r : results) {
            double s = r.getScore() != null ? r.getScore() : 0.0;
            if (s < min) min = s;
            if (s > max) max = s;
        }

        Map<UUID, Double> normalized = new HashMap<>();
        double range = max - min;

        for (DocumentChunkRepository.ChunkSearchResult r : results) {
            double s = r.getScore() != null ? r.getScore() : 0.0;
            double norm = range > 0.00001 ? (s - min) / range : 1.0;
            normalized.put(r.getId(), norm);
        }

        return normalized;
    }

    private String createSnippet(String text) {
        if (text == null) return "";
        String trimmed = text.trim();
        int maxSnippetLength = 250;
        if (trimmed.length() <= maxSnippetLength) {
            return trimmed;
        }
        return trimmed.substring(0, maxSnippetLength) + "...";
    }
}
