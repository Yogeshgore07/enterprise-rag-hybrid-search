package com.yogesh.ragassistant.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.yogesh.ragassistant.dto.response.DocumentResponse;
import com.yogesh.ragassistant.embedding.EmbeddingService;
import com.yogesh.ragassistant.entity.Document;
import com.yogesh.ragassistant.entity.DocumentStatus;
import com.yogesh.ragassistant.mapper.DocumentMapper;
import com.yogesh.ragassistant.rag.chunking.ChunkWithMetadata;
import com.yogesh.ragassistant.rag.chunking.RecursiveCharacterTextSplitter;
import com.yogesh.ragassistant.rag.parser.DocumentParser;
import com.yogesh.ragassistant.rag.parser.DocumentParserFactory;
import com.yogesh.ragassistant.rag.parser.ParsedDocument;
import com.yogesh.ragassistant.repository.DocumentChunkRepository;
import com.yogesh.ragassistant.repository.DocumentRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DocumentServiceTest {

    @Mock
    private DocumentRepository documentRepository;

    @Mock
    private DocumentChunkRepository chunkRepository;

    @Mock
    private DocumentParserFactory parserFactory;

    @Mock
    private DocumentParser documentParser;

    @Mock
    private RecursiveCharacterTextSplitter textSplitter;

    @Mock
    private EmbeddingService embeddingService;

    @Mock
    private DocumentMapper documentMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private DocumentService documentService;

    @Test
    @DisplayName("Should successfully ingest and chunk uploaded text document")
    void testProcessAndStoreDocument() throws Exception {
        String content = "Enterprise Knowledge Management Guidelines. Chapter 1: Standard Protocols.";
        MockMultipartFile file = new MockMultipartFile(
                "file", "protocol.txt", "text/plain", content.getBytes(StandardCharsets.UTF_8));

        Document doc = Document.builder()
                .id(UUID.randomUUID())
                .title("protocol")
                .filename("protocol.txt")
                .fileType("TXT")
                .fileSize((long) content.length())
                .status(DocumentStatus.PROCESSING)
                .build();

        ParsedDocument parsedDoc = ParsedDocument.builder()
                .filename("protocol.txt")
                .pages(List.of(new ParsedDocument.ParsedPage(1, content)))
                .build();

        ChunkWithMetadata chunk = ChunkWithMetadata.builder()
                .chunkIndex(0)
                .pageNumber(1)
                .documentTitle("protocol")
                .heading("General")
                .text(content)
                .tokenCount(15)
                .build();

        DocumentResponse docResponse = DocumentResponse.builder()
                .id(doc.getId())
                .title("protocol")
                .filename("protocol.txt")
                .fileType("TXT")
                .status(DocumentStatus.COMPLETED)
                .chunkCount(1)
                .build();

        when(documentRepository.save(any(Document.class))).thenReturn(doc);
        when(parserFactory.getParser("protocol.txt")).thenReturn(documentParser);
        when(documentParser.parse(any(InputStream.class), eq("protocol.txt"))).thenReturn(parsedDoc);
        when(textSplitter.splitTextWithMetadata(anyString(), anyString(), anyInt(), anyInt())).thenReturn(List.of(chunk));
        when(embeddingService.generateEmbeddings(anyList())).thenReturn(List.of(new float[1536]));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"pageNumber\":1}");
        when(documentMapper.toResponse(any(Document.class))).thenReturn(docResponse);

        DocumentResponse result = documentService.processAndStoreDocument(file, "protocol");

        assertNotNull(result);
        assertEquals(DocumentStatus.COMPLETED, result.getStatus());
        assertEquals(1, result.getChunkCount());
        verify(chunkRepository, times(1)).insertChunkWithVector(any(), any(), anyInt(), anyInt(), anyString(), anyString(), anyString());
    }
}
