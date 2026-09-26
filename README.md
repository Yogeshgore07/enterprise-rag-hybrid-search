# Enterprise Knowledge Assistant – Production RAG Pipeline with Hybrid Search

[![Java](https://img.shields.io/badge/Java-21-orange.svg)](https://www.oracle.com/java/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4.3-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16%20%2B%20pgvector-blue.svg)](https://github.com/pgvector/pgvector)
[![Build](https://img.shields.io/badge/Build-Passing-success.svg)]()
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

An enterprise-grade Retrieval-Augmented Generation (RAG) backend engineered in **Java 21** and **Spring Boot 3.4.x** that performs hybrid search over corporate documents using **PostgreSQL 16 + pgvector** and **PostgreSQL Full-Text Search (TSVECTOR)**. 

Answers are strictly grounded in retrieved documents with **verified page-level citations and snippet excerpts**, preventing hallucinations and ensuring enterprise compliance.

---

## 📑 Table of Contents

- [Architectural Overview](#-architectural-overview)
- [Key Features](#-key-features)
- [Tech Stack](#-tech-stack)
- [Hybrid Search Mechanics](#-hybrid-search-mechanics)
- [Chunking & Document Processing Pipeline](#-chunking--document-processing-pipeline)
- [Project Structure](#-project-structure)
- [Getting Started](#-getting-started)
  - [Prerequisites](#prerequisites)
  - [Docker Compose Quickstart (Recommended)](#docker-compose-quickstart-recommended)
  - [Local Manual Setup](#local-manual-setup)
- [REST API Reference & cURL Examples](#-rest-api-reference--curl-examples)
- [Configuration Reference](#-configuration-reference)
- [Global Error Handling](#-global-error-handling)
- [Testing](#-testing)

---

## 🏗 Architectural Overview

```
                                  +--------------------------------------------------+
                                  |            CLIENT APPLICATION (REST)             |
                                  +--------------------------------------------------+
                                           |                                   ^
                           1. Document / Query Request              6. Grounded Answer + Citations
                                (HTTP Basic Auth)                              |
                                           v                                   |
+-------------------------------------------------------------------------------------------------------------+
|                                        SPRING BOOT RAG PIPELINE CORE                                        |
|                                                                                                             |
|  [ INGESTION PIPELINE ]                                                                                     |
|   Uploaded File  --->  DocumentParser  ---> RecursiveTextSplitter ---> EmbeddingService ---> Batch Persist  |
|  (PDF/DOCX/CSV/TXT)   (PDFBox / POI)        (600 tokens/120 overlap)   (Mock)              (pgvector + GIN) |
|                                                                                                             |
|  [ HYBRID RETRIEVAL & GENERATION PIPELINE ]                                                                 |
|   User Query  -----> Query Embedding  --------------------+                                                 |
|                             |                             |                                                 |
|                             v                             v                                                 |
|                  +----------------------+      +----------------------+                                     |
|                  | pgvector Cosine Sim  |      | PostgreSQL TSVECTOR  |                                     |
|                  |  (Semantic Search)   |      |   (Keyword Search)   |                                     |
|                  +----------------------+      +----------------------+                                     |
|                             \                             /                                                 |
|                              v                           v                                                  |
|                        +---------------------------------------+                                            |
|                        | Score Normalization & Weighted Fusion |                                            |
|                        |      (0.6 Semantic + 0.4 Keyword)     |                                            |
|                        +---------------------------------------+                                            |
|                                            |                                                                |
|                                            v                                                                |
|                               +-------------------------+                                                   |
|                               | Re-ranking Layer (BGE)  |                                                   |
|                               +-------------------------+                                                   |
|                                            |                                                                |
|                                            v                                                                |
|                               +-------------------------+                                                   |
|                               |  Strict Prompt Builder  |                                                   |
|                               +-------------------------+                                                   |
|                                            |                                                                |
|                                            v                                                                |
|                               +-------------------------+                                                   |
|                               |   LLM (Groq / Llama 3.3)|                                                   |
|                               +-------------------------+                                                   |
|                                            |                                                                |
|                                            v                                                                |
|                               +-------------------------+                                                   |
|                               | Citation Verifier Engine|                                                   |
|                               +-------------------------+                                                   |
+-------------------------------------------------------------------------------------------------------------+
                                      |                       ^
                                      v                       |
                        +----------------------------------------------------+
                        |            POSTGRESQL 16 + PGVECTOR DB             |
                        | - document_chunks (embedding VECTOR(1536), GIN)    |
                        | - documents, chat_history, citations, users        |
                        | - Hibernate DDL schema auto-generation             |
                        +----------------------------------------------------+
```

---

## 🚀 Key Features

1. **Hybrid Retrieval (pgvector + TSVECTOR)**:
   - **Semantic Search**: Vector cosine similarity using `pgvector` HNSW index on `VECTOR(1536)` embeddings.
   - **Keyword Search**: Exact term and phrase matching with `tsvector`, `plainto_tsquery`, and `ts_rank_cd`.
   - **Score Normalization & Fusion**: Weighted linear score merging with deduplication.
2. **Recursive Character Text Splitter**:
   - Semantic boundary-aware splitting (`\n\n`, `\n`, sentence stops, words).
   - Configurable chunk size (default: 600 tokens) and overlap (default: 120 tokens).
   - Extracts and preserves section headings, document titles, page numbers, and chunk indices.
3. **Multi-Format Document Parsing**:
   - Native extractors for **PDF** (Apache PDFBox 3.x with per-page tracking), **DOCX** (Apache POI XWPF), **TXT**, and **CSV** (OpenCSV table row transformation).
4. **Pluggable AI Providers**:
   - **Embeddings**: Offline deterministic `mock` vector embedding service.
   - **LLMs**: High-speed **Groq** (`llama-3.3-70b-versatile`) and offline `mock`.
5. **Strict Grounded Generation & Citation Verification**:
   - Anti-hallucination prompt constraints forbidding extrapolation.
   - Verifies and attaches citations with document name, page number, chunk ID, and exact textual snippet.
6. **Pure Spring Security with HTTP Basic Authentication**:
   - Clean, lightweight Basic Auth configured in `SecurityConfig.java` without JWT tokens.
   - Role-Based Access Control (`ROLE_USER`, `ROLE_ADMIN`).
7. **Hibernate DDL Auto-Generation (No Flyway)**:
   - Tables and relationships managed by Hibernate (`spring.jpa.hibernate.ddl-auto=update`).
   - Extensions (`vector`, `pgcrypto`) and full-text search triggers maintained seamlessly by `DatabaseInitializer.java`.
8. **Centralized `@RestControllerAdvice` Exception Handling**:
   - Catches and maps all system and validation exceptions to structured `ErrorDetails` JSON payloads.

---

## 💻 Tech Stack

| Component | Technology | Version |
| :--- | :--- | :--- |
| **Language** | Java | 21 (LTS) |
| **Framework** | Spring Boot | 3.4.3 |
| **Security** | Spring Security (HTTP Basic Auth) | 6.4.x |
| **Database** | PostgreSQL + pgvector extension | 16 |
| **ORM & DDL** | Spring Data JPA (Hibernate 6) | `ddl-auto=update` |
| **Vector Library** | pgvector-java | 0.1.6 |
| **Configuration** | `application.properties` | Flat properties |
| **Document Parsers** | Apache PDFBox, Apache POI, OpenCSV | 3.0.4 / 5.4.0 / 5.9 |
| **API Documentation**| SpringDoc OpenAPI / Swagger UI | 2.8.5 |
| **Build & Test** | Maven, JUnit 5, MockMvc, Mockito | 3.9+ |
| **Deployment** | Docker & Docker Compose | Multi-stage |

---

## 🧮 Hybrid Search Mechanics

Traditional vector search alone often fails on specific part numbers, SKUs, acronyms, or exact keyword queries. Pure keyword search fails on synonyms and semantic meaning. Our **Hybrid Retriever** unites both:

### 1. Vector Cosine Similarity
$$\text{Score}_{\text{semantic}} = 1 - \text{cosine\_distance}(\vec{v}_{\text{query}}, \vec{v}_{\text{chunk}})$$

Executed directly inside PostgreSQL using HNSW index:
```sql
SELECT c.id, d.title, c.page_number, c.chunk_text,
       (1 - (c.embedding <=> CAST(:queryVector AS vector))) AS score
FROM document_chunks c
JOIN documents d ON c.document_id = d.id
ORDER BY c.embedding <=> CAST(:queryVector AS vector) ASC
LIMIT :topK;
```

### 2. Full-Text Search (Cover Density Rank)
$$\text{Score}_{\text{keyword}} = \text{ts\_rank\_cd}(\text{search\_vector}, \text{plainto\_tsquery}(\text{query}))$$

Executed via GIN indexed `tsvector`:
```sql
SELECT c.id, d.title, c.page_number, c.chunk_text,
       ts_rank_cd(c.search_vector, plainto_tsquery('english', :queryText)) AS score
FROM document_chunks c
JOIN documents d ON c.document_id = d.id
WHERE c.search_vector @@ plainto_tsquery('english', :queryText)
ORDER BY score DESC
LIMIT :topK;
```

### 3. Min-Max Normalization & Weighted Fusion
Both raw score distributions are mapped to $[0, 1]$:
$$\tilde{S} = \frac{S - S_{\min}}{S_{\max} - S_{\min}}$$

Combined score calculation (defaults: $w_{\text{vec}} = 0.6, w_{\text{key}} = 0.4$):
$$\text{Score}_{\text{hybrid}} = (w_{\text{vec}} \cdot \tilde{S}_{\text{semantic}}) + (w_{\text{key}} \cdot \tilde{S}_{\text{keyword}})$$

---

## 📦 Project Structure

```
enterprise-rag-hybrid-search/
├── src/
│   ├── main/
│   │   ├── java/com/yogesh/ragassistant/
│   │   │   ├── RagAssistantApplication.java       # Spring Boot main entrypoint
│   │   │   ├── config/                           # Properties, OpenAPI, AppConfig, JpaConfig, DatabaseInitializer
│   │   │   ├── controller/                       # REST APIs (Auth, Docs, Chat, Admin)
│   │   │   ├── dto/                              # Request / Response DTOs
│   │   │   ├── entity/                           # JPA Entities (User, Document, Chunk, Citation)
│   │   │   ├── exception/                        # @RestControllerAdvice GlobalExceptionHandler
│   │   │   ├── mapper/                           # Entity-to-DTO Mappers
│   │   │   ├── repository/                       # Native SQL pgvector & tsvector Repositories
│   │   │   ├── security/                         # Basic Auth SecurityConfig, CustomUserDetailsService
│   │   │   ├── service/                          # AuthService, DocumentService, RagPipelineService
│   │   │   ├── util/                             # VectorUtils, TokenUtils
│   │   │   ├── embedding/                        # EmbeddingService, Mock Provider
│   │   │   ├── rag/
│   │   │   │   ├── chunking/                     # RecursiveCharacterTextSplitter
│   │   │   │   ├── parser/                       # PDF, DOCX, TXT, CSV Document Parsers
│   │   │   │   └── llm/                          # Groq, Mock LLM Services
│   │   │   ├── retriever/                        # HybridRetriever with Score Normalization
│   │   │   ├── reranker/                         # RerankService, NoOp & BGE Reranker
│   │   │   ├── prompt/                           # Strict Anti-Hallucination Prompt Builder
│   │   │   ├── citation/                         # Citation Verification Service
│   │   │   └── history/                          # Chat History Service
│   │   └── resources/
│   │       └── application.properties            # Flat Spring Boot properties
│   └── test/
│       ├── java/                                 # Comprehensive Unit & MockMvc tests
│       └── resources/
│           └── application-test.properties       # H2 Test properties
├── Dockerfile                                    # Multi-stage production container
├── docker-compose.yml                            # PostgreSQL pgvector + pgAdmin + Backend
├── init-db.sql                                   # DB extension initialization script
├── .env.example                                  # Environment variables template
├── pom.xml                                       # Maven build descriptor
└── README.md
```

---

## ⚡ Getting Started

### Prerequisites

* **Java 21 (LTS)** or higher
* **Maven 3.9+**
* **Docker & Docker Compose** (Optional, for containerized run)
* **Groq API Key** (or use built-in offline mock provider)

---

### Docker Compose Quickstart (Recommended)

1. **Clone the repository**:
   ```bash
   git clone https://github.com/Yogeshgore07/enterprise-rag-hybrid-search.git
   cd enterprise-rag-hybrid-search
   ```

2. **Configure Environment Variables**:
   ```bash
   cp .env.example .env
   # Edit .env and enter your GROQ_API_KEY
   ```

3. **Start the Stack**:
   ```bash
   docker-compose up --build -d
   ```

4. **Verify running services**:
   ```bash
   docker-compose ps
   ```
   - **Web Assistant UI**: `http://localhost:8501/`
   - **Swagger UI**: `http://localhost:8501/swagger-ui.html`
   - **pgAdmin 4**: `http://localhost:5050` (Login: `admin@rag.com` / `admin`)

---

### Local Manual Setup

1. **Start PostgreSQL with pgvector**:
   ```bash
   docker run -d --name local-pgvector -p 5432:5432 \
     -e POSTGRES_DB=ragdb \
     -e POSTGRES_USER=raguser \
     -e POSTGRES_PASSWORD=ragpassword \
     pgvector/pgvector:pg16
   ```

2. **Run Maven build & tests**:
   ```bash
   mvn clean test
   ```

3. **Launch Application**:
   ```bash
   mvn spring-boot:run
   ```

---

## 📡 REST API Reference & cURL Examples

Default seeded credentials:
- **Admin**: `admin@company.com` / `Password@123`
- **Standard User**: `user@company.com` / `Password@123`

All authenticated endpoints accept standard **HTTP Basic Authentication** (`-u username:password`).

### 1. Register User
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "analyst@company.com",
    "password": "Password@123",
    "fullName": "Enterprise Analyst",
    "role": "ROLE_USER"
  }'
```

### 2. Login / Verify Credentials
```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@company.com",
    "password": "Password@123"
  }'
```

---

### 3. Upload & Index Document (PDF, DOCX, TXT, CSV)
```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -u admin@company.com:Password@123 \
  -F "file=@/path/to/Corporate_Security_Policy.pdf" \
  -F "title=Corporate Security Policy"
```

**Response:**
```json
{
  "success": true,
  "message": "Document processed and indexed successfully",
  "data": {
    "id": "e4b6c310-863a-4b95-a130-9df24386e8bb",
    "title": "Corporate Security Policy",
    "filename": "Corporate_Security_Policy.pdf",
    "fileType": "PDF",
    "fileSize": 1048576,
    "chunkCount": 18,
    "status": "COMPLETED",
    "errorMessage": null,
    "createdAt": "2026-09-26T00:15:00Z"
  }
}
```

---

### 4. Ask Grounded Question with Hybrid Search
```bash
curl -X POST http://localhost:8080/api/chat/query \
  -u user@company.com:Password@123 \
  -H "Content-Type: application/json" \
  -d '{
    "query": "What is the policy regarding API key rotation?",
    "topK": 10,
    "vectorWeight": 0.6,
    "keywordWeight": 0.4
  }'
```

**Response with Grounded Citations:**
```json
{
  "success": true,
  "message": "Answer generated successfully",
  "data": {
    "queryId": "38a84614-7299-4d83-9b24-7eb3562d942e",
    "conversationId": "conv-101",
    "question": "What is the policy regarding API key rotation?",
    "answer": "According to the Corporate Security Policy, all production API keys must be rotated every 90 days. In the event of a security incident or suspected compromise, keys must be revoked immediately and rotated within 12 hours [Doc: Corporate Security Policy, Page: 4, Chunk: 7].",
    "citations": [
      {
        "citationId": "1a5cbef8-79eb-4a25-8321-df5fa848d795",
        "documentId": "e4b6c310-863a-4b95-a130-9df24386e8bb",
        "chunkId": "f784d1bc-c32f-410a-b302-3c2fa4bcf081",
        "documentTitle": "Corporate Security Policy",
        "pageNumber": 4,
        "chunkIndex": 7,
        "snippet": "Section 4.2 Credential Lifecycle Management. All production API keys and access tokens must be rotated every 90 days without exception...",
        "relevanceScore": 0.942
      }
    ],
    "latencyMs": 842,
    "retrievedChunksCount": 5
  }
}
```

---

### 5. Get User Chat History
```bash
curl -X GET http://localhost:8080/api/chat/history \
  -u user@company.com:Password@123
```

---

### 6. Admin System Statistics
```bash
curl -X GET http://localhost:8080/api/admin/stats \
  -u admin@company.com:Password@123
```

---

## ⚙ Configuration Reference (`application.properties`)

All settings can be configured via `src/main/resources/application.properties` or overridden via environment variables:

| Property | Default | Description |
| :--- | :--- | :--- |
| `spring.jpa.hibernate.ddl-auto` | `update` | Hibernate schema management |
| `rag.chunking.chunk-size` | `600` | Target tokens per chunk (~2400 characters) |
| `rag.chunking.chunk-overlap` | `120` | Tokens overlap between adjacent chunks |
| `rag.embedding.provider` | `mock` | Embedding provider (`mock`) |
| `rag.llm.provider` | `groq` | LLM provider (`groq`, `mock`) |
| `rag.llm.groq.model` | `llama-3.3-70b-versatile` | Groq LLM model identifier |
| `rag.retrieval.top-k` | `15` | Initial candidate pool per search engine |
| `rag.retrieval.final-k` | `5` | Top chunks injected into context prompt |
| `rag.retrieval.vector-weight`| `0.6` | Cosine similarity weight in hybrid search |
| `rag.retrieval.keyword-weight`| `0.4` | Full-text `ts_rank_cd` weight in hybrid search |
| `rag.reranker.enabled` | `false` | Enable/disable secondary cross-encoder |
| `rag.reranker.provider` | `noop` | Reranker implementation (`noop` or `bge`) |

---

## 🛡 Global Error Handling (`@RestControllerAdvice`)

All exceptions thrown across any endpoint are caught by [`GlobalExceptionHandler.java`](file:///c:/Users/gore2/Desktop/RAG/enterprise-rag-hybrid-search/src/main/java/com/yogesh/ragassistant/exception/GlobalExceptionHandler.java) and converted to standardized JSON:

```json
{
  "status": 404,
  "error": "Not Found",
  "message": "Document not found with id : 'e4b6c310-863a-4b95-a130-9df24386e8bb'",
  "path": "/api/documents/e4b6c310-863a-4b95-a130-9df24386e8bb",
  "validationErrors": null,
  "timestamp": "2026-09-26T00:20:00Z"
}
```

---

## 🧪 Testing

The repository features comprehensive unit and MockMvc integration tests:

```bash
# Run all unit and integration tests
mvn clean test
```

Test Coverage Includes:
- **`RecursiveCharacterTextSplitterTest`**: Token estimation, boundary hierarchy, overlap preservation, heading extraction.
- **`HybridRetrieverTest`**: Vector cosine similarity + TSVECTOR keyword search merging, score normalization, deduplication.
- **`PromptBuilderTest`**: Strict grounding template compliance, citation markers.
- **`AuthControllerTest`**: Basic Auth registration, input validation, and profile flows.
- **`DocumentServiceTest`**: Ingestion, page parsing, embedding generation, and metadata database persistence.

---

## 📄 License

Licensed under the [Apache License, Version 2.0](LICENSE).