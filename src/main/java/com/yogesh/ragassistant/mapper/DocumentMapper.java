package com.yogesh.ragassistant.mapper;

import com.yogesh.ragassistant.dto.response.DocumentResponse;
import com.yogesh.ragassistant.entity.Document;
import org.springframework.stereotype.Component;

@Component
public class DocumentMapper {

    public DocumentResponse toResponse(Document document) {
        if (document == null) {
            return null;
        }

        return DocumentResponse.builder()
                .id(document.getId())
                .title(document.getTitle())
                .filename(document.getFilename())
                .fileType(document.getFileType())
                .fileSize(document.getFileSize())
                .chunkCount(document.getChunkCount())
                .status(document.getStatus())
                .errorMessage(document.getErrorMessage())
                .uploadedBy(document.getUser() != null ? document.getUser().getEmail() : "System")
                .userId(document.getUser() != null ? document.getUser().getId() : null)
                .createdAt(document.getCreatedAt())
                .updatedAt(document.getUpdatedAt())
                .build();
    }
}
