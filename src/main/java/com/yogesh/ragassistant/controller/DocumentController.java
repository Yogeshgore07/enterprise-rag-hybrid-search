package com.yogesh.ragassistant.controller;

import com.yogesh.ragassistant.dto.response.ApiResponse;
import com.yogesh.ragassistant.dto.response.DocumentResponse;
import com.yogesh.ragassistant.service.DocumentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Tag(name = "Documents", description = "Endpoints for uploading, listing, and managing enterprise documents")
public class DocumentController {

    private final DocumentService documentService;

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Upload and process document", description = "Uploads a PDF, DOCX, TXT, or CSV document, chunks it, generates embeddings, and saves into pgvector")
    public ResponseEntity<ApiResponse<DocumentResponse>> uploadDocument(
            @Parameter(description = "Document file to upload (PDF, DOCX, TXT, CSV)")
            @RequestParam("file") MultipartFile file,
            @Parameter(description = "Optional document title override")
            @RequestParam(value = "title", required = false) String title) {

        DocumentResponse response = documentService.processAndStoreDocument(file, title);
        return new ResponseEntity<>(ApiResponse.success(response, "Document processed and indexed successfully"), HttpStatus.CREATED);
    }

    @GetMapping
    @Operation(summary = "List all documents", description = "Retrieves all indexed enterprise documents and their status")
    public ResponseEntity<ApiResponse<List<DocumentResponse>>> getAllDocuments() {
        List<DocumentResponse> documents = documentService.getAllDocuments();
        return ResponseEntity.ok(ApiResponse.success(documents));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get document by ID", description = "Retrieves details of a specific document")
    public ResponseEntity<ApiResponse<DocumentResponse>> getDocumentById(@PathVariable UUID id) {
        DocumentResponse document = documentService.getDocumentById(id);
        return ResponseEntity.ok(ApiResponse.success(document));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete document", description = "Deletes a document and cascades to all its embedded chunks")
    public ResponseEntity<ApiResponse<Void>> deleteDocument(@PathVariable UUID id) {
        documentService.deleteDocument(id);
        return ResponseEntity.ok(ApiResponse.success(null, "Document deleted successfully"));
    }
}
