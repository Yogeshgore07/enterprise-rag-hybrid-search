package com.yogesh.ragassistant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatsResponse {

    private long totalUsers;

    private long totalDocuments;

    private long completedDocuments;

    private long failedDocuments;

    private long totalChunks;

    private long totalQueries;

    private long totalCitations;

    private Map<String, Long> documentTypeBreakdown;

    private String embeddingProvider;

    private String llmProvider;

    private double vectorWeight;

    private double keywordWeight;
}
