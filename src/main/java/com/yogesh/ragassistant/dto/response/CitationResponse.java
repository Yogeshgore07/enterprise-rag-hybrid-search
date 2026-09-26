package com.yogesh.ragassistant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CitationResponse {

    private UUID citationId;

    private UUID documentId;

    private UUID chunkId;

    private String documentTitle;

    private Integer pageNumber;

    private Integer chunkIndex;

    private String snippet;

    private Double relevanceScore;
}
