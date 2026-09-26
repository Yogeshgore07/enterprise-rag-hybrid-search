package com.yogesh.ragassistant.repository;

import com.yogesh.ragassistant.entity.ChatHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, UUID> {

    List<ChatHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<ChatHistory> findByConversationIdOrderByCreatedAtAsc(String conversationId);

    List<ChatHistory> findByUserIdAndConversationIdOrderByCreatedAtAsc(UUID userId, String conversationId);

    long countByUserId(UUID userId);
}
