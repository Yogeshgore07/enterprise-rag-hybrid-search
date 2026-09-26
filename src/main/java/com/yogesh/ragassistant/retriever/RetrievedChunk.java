package com.yogesh.ragassistant.retriever;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RetrievedChunk {

    private UUID chunkId;

    private UUID documentId;

    private String documentTitle;

    private int pageNumber;

    private int chunkIndex;

    private String chunkText;

    private Double semanticScore;

    private Double keywordScore;

    private Double hybridScore;

    private String snippet;
}
