package com.yogesh.ragassistant.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
@ConfigurationProperties(prefix = "rag")
@Getter
@Setter
public class RagProperties {

    private Chunking chunking = new Chunking();
    private Embedding embedding = new Embedding();
    private Llm llm = new Llm();
    private Retrieval retrieval = new Retrieval();
    private Reranker reranker = new Reranker();

    @Getter
    @Setter
    public static class Chunking {
        private int chunkSize = 600;       // tokens
        private int chunkOverlap = 120;    // tokens
        private List<String> separators = List.of("\n\n", "\n", ". ", "? ", "! ", "; ", " ", "");
    }

    @Getter
    @Setter
    public static class Embedding {
        private String provider = "mock";
        private int dimension = 1536;
    }

    @Getter
    @Setter
    public static class Llm {
        private String provider = "groq";
        private double temperature = 0.1;
        private int maxTokens = 2048;
        private Groq groq = new Groq();

        @Getter
        @Setter
        public static class Groq {
            private String apiKey;
            private String model = "openai/gpt-oss-120b";
            private String baseUrl = "https://api.groq.com/openai/v1";
        }
    }

    @Getter
    @Setter
    public static class Retrieval {
        private int topK = 15;
        private int finalK = 5;
        private double vectorWeight = 0.6;
        private double keywordWeight = 0.4;
        private double minScoreThreshold = 0.10;
    }

    @Getter
    @Setter
    public static class Reranker {
        private boolean enabled = false;
        private String provider = "noop";
        private Bge bge = new Bge();

        @Getter
        @Setter
        public static class Bge {
            private String endpoint = "http://localhost:8001/rerank";
            private String model = "BAAI/bge-reranker-large";
            private int timeoutSeconds = 10;
        }
    }
}
