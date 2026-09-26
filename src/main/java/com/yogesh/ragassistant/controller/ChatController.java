package com.yogesh.ragassistant.controller;

import com.yogesh.ragassistant.dto.request.ChatQueryRequest;
import com.yogesh.ragassistant.dto.response.ApiResponse;
import com.yogesh.ragassistant.dto.response.ChatHistoryResponse;
import com.yogesh.ragassistant.dto.response.ChatResponse;
import com.yogesh.ragassistant.entity.User;
import com.yogesh.ragassistant.history.ChatHistoryService;
import com.yogesh.ragassistant.repository.UserRepository;
import com.yogesh.ragassistant.security.UserPrincipal;
import com.yogesh.ragassistant.service.RagPipelineService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/chat")
@RequiredArgsConstructor
@Tag(name = "Chat & RAG Pipeline", description = "Endpoints for hybrid query search, grounded generation, and conversation history")
public class ChatController {

    private final RagPipelineService ragPipelineService;
    private final ChatHistoryService chatHistoryService;
    private final UserRepository userRepository;

    @PostMapping("/query")
    @Operation(summary = "Ask question over documents", description = "Executes hybrid retrieval, reranking, and grounded LLM generation with verified citations")
    public ResponseEntity<ApiResponse<ChatResponse>> askQuestion(
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody ChatQueryRequest request) {

        User user = userRepository.getReferenceById(principal.getId());
        ChatResponse response = ragPipelineService.executePipeline(user, request);
        return ResponseEntity.ok(ApiResponse.success(response, "Answer generated successfully"));
    }

    @GetMapping("/history")
    @Operation(summary = "Get user query history", description = "Retrieves all past queries, answers, and citations for the authenticated user")
    public ResponseEntity<ApiResponse<List<ChatHistoryResponse>>> getUserChatHistory(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<ChatHistoryResponse> history = chatHistoryService.getUserHistory(principal.getId());
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @GetMapping("/history/{conversationId}")
    @Operation(summary = "Get conversation history by conversation ID", description = "Retrieves chronological messages in a given conversation thread")
    public ResponseEntity<ApiResponse<List<ChatHistoryResponse>>> getConversationHistory(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable String conversationId) {

        List<ChatHistoryResponse> history = chatHistoryService.getConversationHistory(principal.getId(), conversationId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
