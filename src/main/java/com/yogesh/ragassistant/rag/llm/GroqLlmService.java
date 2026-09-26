package com.yogesh.ragassistant.rag.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.yogesh.ragassistant.config.RagProperties;
import com.yogesh.ragassistant.exception.LlmServiceException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;

@Service("groqLlmService")
@RequiredArgsConstructor
@Slf4j
public class GroqLlmService implements LlmService {

    private final RagProperties ragProperties;
    private final RestClient restClient;

    @Override
    public String generateAnswer(String systemPrompt, String userPrompt) {
        RagProperties.Llm.Groq config = ragProperties.getLlm().getGroq();
        String apiKey = config.getApiKey();

        if (apiKey == null || apiKey.isBlank()) {
            throw new LlmServiceException("Groq API Key is missing. Please configure GROQ_API_KEY in your .env or environment.");
        }

        try {
            long startTime = System.currentTimeMillis();
            GroqChatRequest request = new GroqChatRequest(
                    config.getModel(),
                    ragProperties.getLlm().getTemperature(),
                    ragProperties.getLlm().getMaxTokens(),
                    List.of(
                            new Message("system", systemPrompt),
                            new Message("user", userPrompt)
                    )
            );

            GroqChatResponse response = restClient.post()
                    .uri(config.getBaseUrl() + "/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GroqChatResponse.class);

            long duration = System.currentTimeMillis() - startTime;
            log.info("Groq LLM [{}] response received in {} ms", config.getModel(), duration);

            if (response == null || response.getChoices() == null || response.getChoices().isEmpty()) {
                throw new LlmServiceException("Groq returned an empty completion response");
            }

            return response.getChoices().get(0).getMessage().getContent().trim();
        } catch (Exception ex) {
            log.error("Groq chat completion error: {}", ex.getMessage(), ex);
            throw new LlmServiceException("Failed to generate response from Groq: " + ex.getMessage(), ex);
        }
    }

    @Override
    public String getProviderName() {
        return "groq";
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class GroqChatRequest {
        private String model;
        private double temperature;
        @JsonProperty("max_tokens")
        private int maxTokens;
        private List<Message> messages;
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class Message {
        private String role;
        private String content;
    }

    @Data
    public static class GroqChatResponse {
        private List<Choice> choices;
    }

    @Data
    public static class Choice {
        private Message message;
        @JsonProperty("finish_reason")
        private String finishReason;
    }
}
