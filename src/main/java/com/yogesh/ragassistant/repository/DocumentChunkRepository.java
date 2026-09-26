package com.yogesh.ragassistant.repository;

import com.yogesh.ragassistant.entity.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {

    /**
     * Interface projection for search results.
     */
    interface ChunkSearchResult {
        UUID getId();
        UUID getDocumentId();
        String getDocumentTitle();
        Integer getPageNumber();
        Integer getChunkIndex();
        String getChunkText();
        Double getScore();
    }

    /**
     * Semantic Search via pgvector Cosine Distance:
     * Cosine similarity = 1 - cosine_distance (where cosine_distance is <=>)
     */
    @Query(value = """
        SELECT c.id AS id,
               c.document_id AS documentId,
               d.title AS documentTitle,
               c.page_number AS pageNumber,
               c.chunk_index AS chunkIndex,
               c.chunk_text AS chunkText,
               (1 - (c.embedding <=> CAST(:queryVector AS vector))) AS score
        FROM document_chunks c
        JOIN documents d ON c.document_id = d.id
        WHERE (:documentId IS NULL OR c.document_id = :documentId)
        ORDER BY c.embedding <=> CAST(:queryVector AS vector) ASC
        LIMIT :topK
        """, nativeQuery = true)
    List<ChunkSearchResult> searchSemantic(
            @Param("queryVector") String queryVector,
            @Param("documentId") UUID documentId,
            @Param("topK") int topK);

    /**
     * Keyword Search via PostgreSQL Full-Text Search:
     * Uses tsvector, plainto_tsquery, and ts_rank_cd
     */
    @Query(value = """
        SELECT c.id AS id,
               c.document_id AS documentId,
               d.title AS documentTitle,
               c.page_number AS pageNumber,
               c.chunk_index AS chunkIndex,
               c.chunk_text AS chunkText,
               ts_rank_cd(c.search_vector, plainto_tsquery('english', :queryText)) AS score
        FROM document_chunks c
        JOIN documents d ON c.document_id = d.id
        WHERE c.search_vector @@ plainto_tsquery('english', :queryText)
          AND (:documentId IS NULL OR c.document_id = :documentId)
        ORDER BY score DESC
        LIMIT :topK
        """, nativeQuery = true)
    List<ChunkSearchResult> searchKeyword(
            @Param("queryText") String queryText,
            @Param("documentId") UUID documentId,
            @Param("topK") int topK);

    /**
     * Native query to insert a chunk with vector and JSON metadata.
     */
    @Modifying
    @Query(value = """
        INSERT INTO document_chunks (id, document_id, page_number, chunk_index, chunk_text, embedding, metadata, created_at)
        VALUES (:id, :documentId, :pageNumber, :chunkIndex, :chunkText, CAST(:embedding AS vector), CAST(:metadata AS jsonb), CURRENT_TIMESTAMP)
        """, nativeQuery = true)
    void insertChunkWithVector(
            @Param("id") UUID id,
            @Param("documentId") UUID documentId,
            @Param("pageNumber") int pageNumber,
            @Param("chunkIndex") int chunkIndex,
            @Param("chunkText") String chunkText,
            @Param("embedding") String embedding,
            @Param("metadata") String metadata);

    List<DocumentChunk> findByDocumentIdOrderByChunkIndexAsc(UUID documentId);

    long countByDocumentId(UUID documentId);
}
