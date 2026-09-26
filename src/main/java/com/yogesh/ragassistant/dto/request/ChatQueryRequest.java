package com.yogesh.ragassistant.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatQueryRequest {

    @NotBlank(message = "Query cannot be blank")
    private String query;

    private String conversationId; // Optional: If empty or null, a new conversation ID will be generated

    private UUID documentIdFilter;  // Optional: Restrict search to a specific document

    private Integer topK;          // Optional override for top candidates

    private Double vectorWeight;   // Optional override for hybrid weighting

    private Double keywordWeight;  // Optional override for hybrid weighting
}
