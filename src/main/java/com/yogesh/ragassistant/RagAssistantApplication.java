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
        configureDatabaseProperties();
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

    /**
     * Translates cloud provider connection strings (e.g. Render, Railway, Heroku DATABASE_URL)
     * from postgres:// or postgresql:// into JDBC format and extracts credentials if embedded.
     */
    private static void configureDatabaseProperties() {
        String dbUrl = System.getenv("SPRING_DATASOURCE_URL");
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getProperty("SPRING_DATASOURCE_URL");
        }
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getenv("DATABASE_URL");
        }
        if (dbUrl == null || dbUrl.isBlank()) {
            dbUrl = System.getProperty("DATABASE_URL");
        }

        if (dbUrl == null || dbUrl.isBlank()) {
            return;
        }

        dbUrl = dbUrl.trim();

        if (dbUrl.startsWith("postgres://") || dbUrl.startsWith("postgresql://")) {
            try {
                String uriStr = dbUrl.replaceFirst("^postgres(ql)?://", "http://");
                java.net.URI uri = new java.net.URI(uriStr);
                String userInfo = uri.getUserInfo();
                String host = uri.getHost();
                int port = uri.getPort() == -1 ? 5432 : uri.getPort();
                String path = uri.getPath();
                String query = uri.getQuery();

                if (userInfo != null && !userInfo.isBlank()) {
                    String[] credentials = userInfo.split(":", 2);
                    if (System.getProperty("spring.datasource.username") == null && System.getenv("SPRING_DATASOURCE_USERNAME") == null) {
                        System.setProperty("spring.datasource.username", credentials[0]);
                        System.setProperty("SPRING_DATASOURCE_USERNAME", credentials[0]);
                    }
                    if (credentials.length > 1 && System.getProperty("spring.datasource.password") == null && System.getenv("SPRING_DATASOURCE_PASSWORD") == null) {
                        System.setProperty("spring.datasource.password", credentials[1]);
                        System.setProperty("SPRING_DATASOURCE_PASSWORD", credentials[1]);
                    }
                }

                StringBuilder jdbcUrl = new StringBuilder("jdbc:postgresql://")
                        .append(host)
                        .append(":")
                        .append(port)
                        .append(path != null ? path : "");
                if (query != null && !query.isBlank()) {
                    jdbcUrl.append("?").append(query);
                }
                String finalUrl = jdbcUrl.toString();
                System.setProperty("spring.datasource.url", finalUrl);
                System.setProperty("SPRING_DATASOURCE_URL", finalUrl);
            } catch (Exception ex) {
                String fallbackUrl = dbUrl.replaceFirst("^postgres(ql)?://", "jdbc:postgresql://");
                System.setProperty("spring.datasource.url", fallbackUrl);
                System.setProperty("SPRING_DATASOURCE_URL", fallbackUrl);
            }
        } else if (!dbUrl.startsWith("jdbc:")) {
            String finalUrl = "jdbc:" + dbUrl;
            System.setProperty("spring.datasource.url", finalUrl);
            System.setProperty("SPRING_DATASOURCE_URL", finalUrl);
        }
    }
}
