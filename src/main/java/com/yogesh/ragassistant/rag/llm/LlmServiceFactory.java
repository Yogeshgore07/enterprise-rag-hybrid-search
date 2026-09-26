package com.yogesh.ragassistant.rag.llm;

import com.yogesh.ragassistant.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class LlmServiceFactory implements LlmService {

    private final RagProperties ragProperties;
    private final GroqLlmService groqLlmService;
    private final MockLlmService mockLlmService;

    public LlmService getActiveService() {
        String provider = ragProperties.getLlm().getProvider();

        if ("mock".equalsIgnoreCase(provider)) {
            return mockLlmService;
        }

        // Default: Groq
        String apiKey = ragProperties.getLlm().getGroq().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GROQ_API_KEY is not configured. Falling back to MockLlmService.");
            return mockLlmService;
        }

        return groqLlmService;
    }

    @Override
    public String generateAnswer(String systemPrompt, String userPrompt) {
        return getActiveService().generateAnswer(systemPrompt, userPrompt);
    }

    @Override
    public String getProviderName() {
        return getActiveService().getProviderName();
    }
}
