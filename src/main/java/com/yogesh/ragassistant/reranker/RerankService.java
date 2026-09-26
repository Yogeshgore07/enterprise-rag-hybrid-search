package com.yogesh.ragassistant.reranker;

import com.yogesh.ragassistant.retriever.RetrievedChunk;

import java.util.List;

public interface RerankService {

    /**
     * Reranks candidate chunks according to deep relevance against the query.
     */
    List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN);

    /**
     * Identifier of the reranker implementation.
     */
    String getProviderName();
}
