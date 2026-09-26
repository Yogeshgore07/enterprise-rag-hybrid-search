package com.yogesh.ragassistant.reranker;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yogesh.ragassistant.config.RagProperties;
import com.yogesh.ragassistant.retriever.RetrievedChunk;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service("bgeReranker")
@RequiredArgsConstructor
@Slf4j
public class BgeReranker implements RerankService {

    private final RagProperties ragProperties;
    private final RestClient restClient;

    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN) {
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }

        RagProperties.Reranker.Bge bgeConfig = ragProperties.getReranker().getBge();

        try {
            long startTime = System.currentTimeMillis();
            List<String> texts = candidates.stream().map(RetrievedChunk::getChunkText).toList();
            BgeRerankRequest request = new BgeRerankRequest(query, texts);

            BgeRerankResponse response = restClient.post()
                    .uri(bgeConfig.getEndpoint())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(BgeRerankResponse.class);

            long duration = System.currentTimeMillis() - startTime;
            log.info("BGE Reranker returned scores for {} candidates in {} ms", candidates.size(), duration);

            if (response != null && response.getResults() != null) {
                // Update candidates with cross-encoder rerank scores
                for (RerankItem item : response.getResults()) {
                    if (item.getIndex() < candidates.size()) {
                        candidates.get(item.getIndex()).setHybridScore(item.getRelevanceScore());
                    }
                }

                return candidates.stream()
                        .sorted(Comparator.comparingDouble(RetrievedChunk::getHybridScore).reversed())
                        .limit(topN)
                        .toList();
            }
        } catch (Exception ex) {
            log.warn("BGE Reranker service unreachable at {}. Falling back to default hybrid ranking: {}",
                    bgeConfig.getEndpoint(), ex.getMessage());
        }

        // Graceful fallback to initial hybrid candidates
        return candidates.stream().limit(topN).toList();
    }

    @Override
    public String getProviderName() {
        return "bge";
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class BgeRerankRequest {
        private String query;
        private List<String> documents;
    }

    @Data
    public static class BgeRerankResponse {
        private List<RerankItem> results;
    }

    @Data
    public static class RerankItem {
        private int index;
        @JsonProperty("relevance_score")
        private double relevanceScore;
    }
}
