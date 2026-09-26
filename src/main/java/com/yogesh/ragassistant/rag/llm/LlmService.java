package com.yogesh.ragassistant.rag.llm;

public interface LlmService {

    /**
     * Generates a grounded response given the system prompt (context, instructions)
     * and user prompt (query).
     */
    String generateAnswer(String systemPrompt, String userPrompt);

    /**
     * Unique identifier for the provider (e.g. "groq", "mock").
     */
    String getProviderName();
}
