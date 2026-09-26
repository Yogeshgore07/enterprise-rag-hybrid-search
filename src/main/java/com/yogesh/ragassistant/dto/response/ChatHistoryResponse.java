package com.yogesh.ragassistant.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatHistoryResponse {

    private UUID id;

    private String conversationId;

    private String question;

    private String answer;

    @Builder.Default
    private List<CitationResponse> citations = new ArrayList<>();

    private Instant createdAt;
}
