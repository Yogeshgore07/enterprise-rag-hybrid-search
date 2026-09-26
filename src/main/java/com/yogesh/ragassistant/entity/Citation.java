package com.yogesh.ragassistant.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "citations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Citation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "chat_history_id", nullable = false)
    private ChatHistory chatHistory;

    @Column(name = "document_id")
    private UUID documentId;

    @Column(name = "chunk_id")
    private UUID chunkId;

    @Column(name = "document_title", nullable = false)
    private String documentTitle;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "chunk_index", nullable = false)
    private Integer chunkIndex;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String snippet;

    @Column(name = "relevance_score")
    private Double relevanceScore;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;
}
