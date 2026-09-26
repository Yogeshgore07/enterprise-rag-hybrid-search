package com.yogesh.ragassistant.history;

import com.yogesh.ragassistant.dto.response.ChatHistoryResponse;
import com.yogesh.ragassistant.dto.response.CitationResponse;
import com.yogesh.ragassistant.entity.ChatHistory;
import com.yogesh.ragassistant.entity.Citation;
import com.yogesh.ragassistant.entity.User;
import com.yogesh.ragassistant.repository.ChatHistoryRepository;
import com.yogesh.ragassistant.repository.CitationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChatHistoryService {

    private final ChatHistoryRepository chatHistoryRepository;
    private final CitationRepository citationRepository;

    @Transactional
    public ChatHistory saveChatRecord(User user, String conversationId, String question, String answer, List<CitationResponse> citationResponses) {
        ChatHistory chat = ChatHistory.builder()
                .user(user)
                .conversationId(conversationId)
                .question(question)
                .answer(answer)
                .build();

        ChatHistory savedChat = chatHistoryRepository.save(chat);

        if (citationResponses != null && !citationResponses.isEmpty()) {
            List<Citation> citationsToSave = new ArrayList<>();
            for (CitationResponse dto : citationResponses) {
                Citation citation = Citation.builder()
                        .chatHistory(savedChat)
                        .documentId(dto.getDocumentId())
                        .chunkId(dto.getChunkId())
                        .documentTitle(dto.getDocumentTitle())
                        .pageNumber(dto.getPageNumber())
                        .chunkIndex(dto.getChunkIndex())
                        .snippet(dto.getSnippet())
                        .relevanceScore(dto.getRelevanceScore())
                        .build();
                citationsToSave.add(citation);
            }
            citationRepository.saveAll(citationsToSave);
            savedChat.setCitations(citationsToSave);
        }

        log.debug("Persisted chat query for user {} in conversation {}", user.getEmail(), conversationId);
        return savedChat;
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> getUserHistory(UUID userId) {
        List<ChatHistory> history = chatHistoryRepository.findByUserIdOrderByCreatedAtDesc(userId);
        return history.stream().map(this::mapToResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ChatHistoryResponse> getConversationHistory(UUID userId, String conversationId) {
        List<ChatHistory> history = chatHistoryRepository.findByUserIdAndConversationIdOrderByCreatedAtAsc(userId, conversationId);
        return history.stream().map(this::mapToResponse).toList();
    }

    private ChatHistoryResponse mapToResponse(ChatHistory history) {
        List<CitationResponse> citationList = history.getCitations() != null
                ? history.getCitations().stream().map(c -> CitationResponse.builder()
                        .citationId(c.getId())
                        .documentId(c.getDocumentId())
                        .chunkId(c.getChunkId())
                        .documentTitle(c.getDocumentTitle())
                        .pageNumber(c.getPageNumber())
                        .chunkIndex(c.getChunkIndex())
                        .snippet(c.getSnippet())
                        .relevanceScore(c.getRelevanceScore())
                        .build()).toList()
                : List.of();

        return ChatHistoryResponse.builder()
                .id(history.getId())
                .conversationId(history.getConversationId())
                .question(history.getQuestion())
                .answer(history.getAnswer())
                .citations(citationList)
                .createdAt(history.getCreatedAt())
                .build();
    }
}
