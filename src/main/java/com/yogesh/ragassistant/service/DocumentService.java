package com.yogesh.ragassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogesh.ragassistant.dto.response.DocumentResponse;
import com.yogesh.ragassistant.embedding.EmbeddingService;
import com.yogesh.ragassistant.entity.Document;
import com.yogesh.ragassistant.entity.DocumentStatus;
import com.yogesh.ragassistant.exception.DocumentProcessingException;
import com.yogesh.ragassistant.exception.ResourceNotFoundException;
import com.yogesh.ragassistant.mapper.DocumentMapper;
import com.yogesh.ragassistant.rag.chunking.ChunkWithMetadata;
import com.yogesh.ragassistant.rag.chunking.RecursiveCharacterTextSplitter;
import com.yogesh.ragassistant.rag.parser.DocumentParser;
import com.yogesh.ragassistant.rag.parser.DocumentParserFactory;
import com.yogesh.ragassistant.rag.parser.ParsedDocument;
import com.yogesh.ragassistant.repository.DocumentChunkRepository;
import com.yogesh.ragassistant.repository.DocumentRepository;
import com.yogesh.ragassistant.util.VectorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentService {

    private final DocumentRepository documentRepository;
    private final DocumentChunkRepository chunkRepository;
    private final DocumentParserFactory parserFactory;
    private final RecursiveCharacterTextSplitter textSplitter;
    private final EmbeddingService embeddingService;
    private final DocumentMapper documentMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public DocumentResponse processAndStoreDocument(MultipartFile file, String titleOverride) {
        if (file == null || file.isEmpty()) {
            throw new DocumentProcessingException("Uploaded file cannot be empty");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new DocumentProcessingException("File must have a valid filename");
        }

        String extension = originalFilename.substring(originalFilename.lastIndexOf('.') + 1).toLowerCase();
        String title = (titleOverride != null && !titleOverride.isBlank())
                ? titleOverride
                : originalFilename.substring(0, originalFilename.lastIndexOf('.'));

        long fileSize = file.getSize();
        log.info("Starting automated RAG pipeline for document: '{}' ({}, {} bytes)", title, originalFilename, fileSize);

        // 1. Initialize Document Entity
        Document document = Document.builder()
                .title(title)
                .filename(originalFilename)
                .fileType(extension.toUpperCase())
                .fileSize(fileSize)
                .status(DocumentStatus.PROCESSING)
                .chunkCount(0)
                .build();

        Document savedDoc = documentRepository.save(document);

        try {
            long startTime = System.currentTimeMillis();

            // 2. Parse Document into structured pages
            DocumentParser parser = parserFactory.getParser(originalFilename);
            ParsedDocument parsedDoc;
            try (InputStream is = file.getInputStream()) {
                parsedDoc = parser.parse(is, originalFilename);
            }

            if (parsedDoc.getPages().isEmpty()) {
                throw new DocumentProcessingException("No readable text could be extracted from: " + originalFilename);
            }

            // 3. Chunking with Recursive Character Text Splitter
            List<ChunkWithMetadata> allChunks = new ArrayList<>();
            int chunkIndexCounter = 0;

            for (ParsedDocument.ParsedPage page : parsedDoc.getPages()) {
                List<ChunkWithMetadata> pageChunks = textSplitter.splitTextWithMetadata(
                        page.getContent(),
                        title,
                        page.getPageNumber(),
                        chunkIndexCounter
                );
                allChunks.addAll(pageChunks);
                chunkIndexCounter += pageChunks.size();
            }

            if (allChunks.isEmpty()) {
                throw new DocumentProcessingException("Chunking resulted in 0 chunks for document: " + originalFilename);
            }

            log.info("Document '{}' split into {} chunks across {} pages",
                    title, allChunks.size(), parsedDoc.getPages().size());

            // 4. Generate Embeddings in batch
            List<String> chunkTexts = allChunks.stream().map(ChunkWithMetadata::getText).toList();
            List<float[]> embeddings = embeddingService.generateEmbeddings(chunkTexts);

            if (embeddings.size() != allChunks.size()) {
                throw new DocumentProcessingException("Mismatch between chunk count (" + allChunks.size() +
                        ") and generated embeddings (" + embeddings.size() + ")");
            }

            // 5. Insert Chunks with pgvector and metadata JSON
            for (int i = 0; i < allChunks.size(); i++) {
                ChunkWithMetadata chunk = allChunks.get(i);
                float[] vector = embeddings.get(i);
                String vectorStr = VectorUtils.toPgVectorString(vector);

                Map<String, Object> metaMap = new HashMap<>();
                metaMap.put("documentTitle", chunk.getDocumentTitle());
                metaMap.put("pageNumber", chunk.getPageNumber());
                metaMap.put("chunkIndex", chunk.getChunkIndex());
                metaMap.put("heading", chunk.getHeading());
                metaMap.put("tokenCount", chunk.getTokenCount());
                metaMap.putAll(chunk.getAdditionalMetadata());

                String metadataJson = objectMapper.writeValueAsString(metaMap);

                chunkRepository.insertChunkWithVector(
                        UUID.randomUUID(),
                        savedDoc.getId(),
                        chunk.getPageNumber(),
                        chunk.getChunkIndex(),
                        chunk.getText(),
                        vectorStr,
                        metadataJson
                );
            }

            long totalDuration = System.currentTimeMillis() - startTime;
            log.info("Successfully ingested document '{}' with {} chunks in {} ms",
                    title, allChunks.size(), totalDuration);

            // 6. Update Document Status
            savedDoc.setStatus(DocumentStatus.COMPLETED);
            savedDoc.setChunkCount(allChunks.size());
            savedDoc = documentRepository.save(savedDoc);

            return documentMapper.toResponse(savedDoc);

        } catch (Exception ex) {
            log.error("Pipeline failure for document '{}': {}", originalFilename, ex.getMessage(), ex);
            savedDoc.setStatus(DocumentStatus.FAILED);
            savedDoc.setErrorMessage(ex.getMessage());
            documentRepository.save(savedDoc);
            throw new DocumentProcessingException("Failed to process document: " + ex.getMessage(), ex);
        }
    }

    @Transactional(readOnly = true)
    public List<DocumentResponse> getAllDocuments() {
        return documentRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(documentMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public DocumentResponse getDocumentById(UUID id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
        return documentMapper.toResponse(document);
    }

    @Transactional
    public void deleteDocument(UUID id) {
        log.info("Deleting document and all associated chunks for id: {}", id);
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Document", "id", id));
        documentRepository.delete(document);
    }
}
