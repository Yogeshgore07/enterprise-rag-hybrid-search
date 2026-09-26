package com.yogesh.ragassistant.service;

import com.yogesh.ragassistant.config.RagProperties;
import com.yogesh.ragassistant.dto.response.AdminStatsResponse;
import com.yogesh.ragassistant.dto.response.DocumentResponse;
import com.yogesh.ragassistant.entity.DocumentStatus;
import com.yogesh.ragassistant.mapper.DocumentMapper;
import com.yogesh.ragassistant.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminService {

    private final UserRepository userRepository;
    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final ChatHistoryRepository chatHistoryRepository;
    private final CitationRepository citationRepository;
    private final DocumentMapper documentMapper;
    private final RagProperties ragProperties;

    @Transactional(readOnly = true)
    public AdminStatsResponse getSystemStats() {
        long totalUsers = userRepository.count();
        long totalDocs = documentRepository.count();
        long completedDocs = documentRepository.countByStatus(DocumentStatus.COMPLETED);
        long failedDocs = documentRepository.countByStatus(DocumentStatus.FAILED);
        long totalChunks = chunkRepository.count();
        long totalQueries = chatHistoryRepository.count();
        long totalCitations = citationRepository.count();

        Map<String, Long> breakdown = new HashMap<>();
        List<Object[]> rawBreakdown = documentRepository.countGroupedByFileType();
        for (Object[] row : rawBreakdown) {
            String type = (String) row[0];
            Long count = (Long) row[1];
            breakdown.put(type, count);
        }

        return AdminStatsResponse.builder()
                .totalUsers(totalUsers)
                .totalDocuments(totalDocs)
                .completedDocuments(completedDocs)
                .failedDocuments(failedDocs)
                .totalChunks(totalChunks)
                .totalQueries(totalQueries)
                .totalCitations(totalCitations)
                .documentTypeBreakdown(breakdown)
                .embeddingProvider(ragProperties.getEmbedding().getProvider())
                .llmProvider(ragProperties.getLlm().getProvider())
                .vectorWeight(ragProperties.getRetrieval().getVectorWeight())
                .keywordWeight(ragProperties.getRetrieval().getKeywordWeight())
                .build();
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getAdminDocuments() {
        return documentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(documentMapper::toResponse)
                .toList();
    }
}
