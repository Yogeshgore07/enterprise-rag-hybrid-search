package com.yogesh.ragassistant.repository;

import com.yogesh.ragassistant.entity.Document;
import com.yogesh.ragassistant.entity.DocumentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentRepository extends JpaRepository<Document, UUID> {

    List<Document> findAllByOrderByCreatedAtDesc();

    long countByStatus(DocumentStatus status);

    @Query("SELECT d.fileType, COUNT(d) FROM Document d GROUP BY d.fileType")
    List<Object[]> countGroupedByFileType();
}
