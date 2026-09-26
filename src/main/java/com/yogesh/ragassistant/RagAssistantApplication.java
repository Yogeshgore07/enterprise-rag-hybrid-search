package com.yogesh.ragassistant;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Enterprise Knowledge Assistant - Production RAG Pipeline with Hybrid Search
 * 
 * Core Features:
 * - Hybrid Retrieval: pgvector Cosine Similarity + PostgreSQL tsvector Full-Text Search
 * - Production Chunking: Recursive Character Text Splitter with metadata
 * - Modular Embedding & LLM Providers (Groq, Mock)
 * - Strict Grounded Prompting with Citation Verification (Doc title, page, snippet)
 * - Role-Based JWT Security & Full Document Lifecycle Management
 */
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;

@SpringBootApplication
@ConfigurationPropertiesScan
@EnableAsync
public class RagAssistantApplication {

    public static void main(String[] args) {
        loadDotEnvIfPresent();
        SpringApplication.run(RagAssistantApplication.class, args);
    }

    private static void loadDotEnvIfPresent() {
        File envFile = new File(".env");
        if (!envFile.exists()) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new FileReader(envFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty() || line.startsWith("#") || !line.contains("=")) {
                    continue;
                }
                int eq = line.indexOf('=');
                String key = line.substring(0, eq).trim();
                String value = line.substring(eq + 1).trim();

                if ((value.startsWith("\"") && value.endsWith("\"")) ||
                    (value.startsWith("'") && value.endsWith("'"))) {
                    value = value.substring(1, value.length() - 1);
                }

                if (!key.isEmpty() && System.getProperty(key) == null && System.getenv(key) == null) {
                    System.setProperty(key, value);
                }
            }
        } catch (Exception ignored) {
        }
    }
}
