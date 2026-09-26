package com.yogesh.ragassistant.embedding;

import java.util.List;

/**
 * Pluggable Embedding Service interface.
 * Returns float[] vector representations for text.
 */
public interface EmbeddingService {

    /**
     * Generate embedding for a single text chunk.
     */
    float[] generateEmbedding(String text);

    /**
     * Batch generate embeddings for multiple text chunks.
     */
    List<float[]> generateEmbeddings(List<String> texts);

    /**
     * Return vector dimension (e.g. 1536 for text-embedding-3-small).
     */
    int getDimension();

    /**
     * Unique identifier for the provider (e.g. "mock").
     */
    String getProviderName();
}
