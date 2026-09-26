package com.yogesh.ragassistant.mapper;

import com.yogesh.ragassistant.dto.response.ChatResponse;
import com.yogesh.ragassistant.dto.response.CitationResponse;
import com.yogesh.ragassistant.entity.ChatHistory;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ChatMapper {

    public ChatResponse toResponse(ChatHistory history, List<CitationResponse> citations, long latencyMs, int retrievedChunksCount) {
        return ChatResponse.builder()
                .queryId(history.getId())
                .conversationId(history.getConversationId())
                .question(history.getQuestion())
                .answer(history.getAnswer())
                .citations(citations)
                .latencyMs(latencyMs)
                .retrievedChunksCount(retrievedChunksCount)
                .timestamp(Instant.now())
                .build();
    }
}
