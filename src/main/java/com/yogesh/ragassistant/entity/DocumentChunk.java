package com.yogesh.ragassistant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_chunks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DocumentChunk {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "document_id", nullable = false)
    private Document document;

    @Column(name = "page_number", nullable = false)
    @Builder.Default
    private Integer pageNumber = 1;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(name = "chunk_text", columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    /**
     * Stored in PostgreSQL as VECTOR(1536).
     * String representation is "[0.123,0.456,...]".
     */
    @Column(name = "embedding", columnDefinition = "vector(1536)")
    private String embedding;

    /**
     * Metadata JSON containing heading, document title, tokens count, etc.
     */
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;

    /**
     * PostgreSQL tsvector for Full-Text Search.
     * Maintained automatically by DB trigger.
     */
    @Column(name = "search_vector", columnDefinition = "tsvector", insertable = false, updatable = false)
    private String searchVector;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
