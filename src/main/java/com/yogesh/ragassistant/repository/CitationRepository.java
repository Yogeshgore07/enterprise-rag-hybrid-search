package com.yogesh.ragassistant.repository;

import com.yogesh.ragassistant.entity.Citation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface CitationRepository extends JpaRepository<Citation, UUID> {

    List<Citation> findByChatHistoryId(UUID chatHistoryId);

    long countByDocumentId(UUID documentId);
}
