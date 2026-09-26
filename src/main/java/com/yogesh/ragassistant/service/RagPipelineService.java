package com.yogesh.ragassistant.service;

import com.yogesh.ragassistant.citation.CitationVerificationService;
import com.yogesh.ragassistant.config.RagProperties;
import com.yogesh.ragassistant.dto.request.ChatQueryRequest;
import com.yogesh.ragassistant.dto.response.ChatResponse;
import com.yogesh.ragassistant.dto.response.CitationResponse;
import com.yogesh.ragassistant.embedding.EmbeddingService;
import com.yogesh.ragassistant.entity.ChatHistory;
import com.yogesh.ragassistant.entity.User;
import com.yogesh.ragassistant.history.ChatHistoryService;
import com.yogesh.ragassistant.mapper.ChatMapper;
import com.yogesh.ragassistant.prompt.PromptBuilder;
import com.yogesh.ragassistant.rag.llm.LlmService;
import com.yogesh.ragassistant.entity.Role;
import com.yogesh.ragassistant.repository.DocumentRepository;
import com.yogesh.ragassistant.reranker.RerankService;
import com.yogesh.ragassistant.retriever.HybridRetriever;
import com.yogesh.ragassistant.retriever.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RagPipelineService {

    private final EmbeddingService embeddingService;
    private final HybridRetriever hybridRetriever;
    private final RerankService rerankService;
    private final PromptBuilder promptBuilder;
    private final LlmService llmService;
    private final CitationVerificationService citationVerificationService;
    private final ChatHistoryService chatHistoryService;
    private final ChatMapper chatMapper;
    private final RagProperties ragProperties;
    private final DocumentRepository documentRepository;

    public ChatResponse executePipeline(User user, ChatQueryRequest request) {
        long startTime = System.currentTimeMillis();
        String query = request.getQuery().trim();
        String conversationId = (request.getConversationId() != null && !request.getConversationId().isBlank())
                ? request.getConversationId()
                : UUID.randomUUID().toString();

        boolean isAdmin = user.getRole() == Role.ROLE_ADMIN;
        UUID userIdFilter = isAdmin ? null : user.getId();

        // Security check: non-admin users can only set scope to docs they ingested
        if (!isAdmin && request.getDocumentIdFilter() != null) {
            if (!documentRepository.existsByIdAndUserId(request.getDocumentIdFilter(), user.getId())) {
                throw new IllegalArgumentException("Access Denied: You can only query documents that you ingested.");
            }
        }

        log.info("Executing RAG Pipeline for query: '{}' [User: {}, Role: {}, Conv: {}, DocumentScope: {}, UserScope: {}]",
                query, user.getEmail(), user.getRole(), conversationId, request.getDocumentIdFilter(), userIdFilter);

        // 1. Generate Query Vector Embedding
        long embedStart = System.currentTimeMillis();
        float[] queryEmbedding = embeddingService.generateEmbedding(query);
        long embedDuration = System.currentTimeMillis() - embedStart;
        log.debug("Query embedding generated in {} ms", embedDuration);

        // 2. Hybrid Retrieval (pgvector + PostgreSQL full-text search)
        long retrieveStart = System.currentTimeMillis();
        List<RetrievedChunk> candidateChunks = hybridRetriever.retrieve(
                query,
                queryEmbedding,
                request.getDocumentIdFilter(),
                userIdFilter,
                request.getTopK(),
                request.getVectorWeight(),
                request.getKeywordWeight()
        );
        long retrieveDuration = System.currentTimeMillis() - retrieveStart;
        log.debug("Hybrid retrieval completed in {} ms (found {} candidates)",
                retrieveDuration, candidateChunks.size());

        // 3. Optional Re-ranking (BGE or NoOp)
        int finalK = ragProperties.getRetrieval().getFinalK();
        List<RetrievedChunk> topChunks = rerankService.rerank(query, candidateChunks, finalK);
        log.debug("Reranking selected top {} chunks", topChunks.size());

        // 4. Build Strict Grounded Prompts
        String systemPrompt = promptBuilder.buildSystemPrompt(topChunks);
        String userPrompt = promptBuilder.buildUserPrompt(query);

        // 5. LLM Answer Generation
        long llmStart = System.currentTimeMillis();
        String answer = llmService.generateAnswer(systemPrompt, userPrompt);
        long llmDuration = System.currentTimeMillis() - llmStart;
        log.info("LLM [{}] generated answer in {} ms", llmService.getProviderName(), llmDuration);

        // 6. Citation Verification and Snippet Extraction
        List<CitationResponse> citations = citationVerificationService.verifyAndBuildCitations(answer, topChunks);

        // 7. Persist Conversation & Citations in Database
        ChatHistory savedChat = chatHistoryService.saveChatRecord(
                user,
                conversationId,
                query,
                answer,
                citations
        );

        long totalLatency = System.currentTimeMillis() - startTime;
        log.info("RAG Pipeline execution finished in {} ms ({} citations attached)",
                totalLatency, citations.size());

        return chatMapper.toResponse(savedChat, citations, totalLatency, topChunks.size());
    }
}
