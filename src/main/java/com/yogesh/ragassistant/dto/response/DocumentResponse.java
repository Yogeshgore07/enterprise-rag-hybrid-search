package com.yogesh.ragassistant.dto.response;

import com.yogesh.ragassistant.entity.DocumentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DocumentResponse {

    private UUID id;

    private String title;

    private String filename;

    private String fileType;

    private Long fileSize;

    private Integer chunkCount;

    private DocumentStatus status;

    private String errorMessage;

    private String uploadedBy;
    private UUID userId;

    private Instant createdAt;

    private Instant updatedAt;
}
