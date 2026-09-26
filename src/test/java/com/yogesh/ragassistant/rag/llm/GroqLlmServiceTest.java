package com.yogesh.ragassistant.rag.llm;

import com.yogesh.ragassistant.config.RagProperties;
import com.yogesh.ragassistant.exception.LlmServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class GroqLlmServiceTest {

    @Mock
    private RestClient restClient;

    private RagProperties ragProperties;
    private GroqLlmService groqLlmService;

    @BeforeEach
    void setUp() {
        ragProperties = new RagProperties();
        groqLlmService = new GroqLlmService(ragProperties, restClient);
    }

    @Test
    @DisplayName("Should return 'groq' as the provider name")
    void testGetProviderName() {
        assertEquals("groq", groqLlmService.getProviderName());
    }

    @Test
    @DisplayName("Should throw LlmServiceException when Groq API key is missing")
    void testGenerateAnswerThrowsWhenApiKeyMissing() {
        ragProperties.getLlm().getGroq().setApiKey(null);

        LlmServiceException exception = assertThrows(
                LlmServiceException.class,
                () -> groqLlmService.generateAnswer("System instructions", "User query")
        );

        assertTrue(exception.getMessage().contains("Groq API Key is missing"));
    }

    @Test
    @DisplayName("Should throw LlmServiceException when Groq API key is blank")
    void testGenerateAnswerThrowsWhenApiKeyBlank() {
        ragProperties.getLlm().getGroq().setApiKey("   ");

        LlmServiceException exception = assertThrows(
                LlmServiceException.class,
                () -> groqLlmService.generateAnswer("System instructions", "User query")
        );

        assertTrue(exception.getMessage().contains("Groq API Key is missing"));
    }
}
