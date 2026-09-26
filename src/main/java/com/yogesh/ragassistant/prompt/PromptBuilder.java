package com.yogesh.ragassistant.prompt;

import com.yogesh.ragassistant.retriever.RetrievedChunk;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Builds grounded prompts for the LLM ensuring strict compliance to provided context
 * and accurate citation markers.
 */
@Component
public class PromptBuilder {

    private static final String SYSTEM_PROMPT_TEMPLATE = """
        You are an Enterprise Knowledge Assistant. Your primary responsibility is to provide accurate, factual, and strictly grounded answers based ONLY on the provided document excerpts below.

        CRITICAL OPERATIONAL RULES:
        1. Answer ONLY using the facts stated in the RETRIEVED CONTEXT below.
        2. If the context does not contain enough information to answer the question truthfully and completely, state clearly: "I cannot answer this question based on the provided documents."
        3. Do NOT make up, assume, speculate, or extrapolate facts beyond what is explicitly written in the context.
        4. When referencing information from a specific chunk, append the citation marker at the end of the sentence or fact, formatted as [Doc: {Document Title}, Page: {Page Number}, Chunk: {Chunk Index}].
        5. Maintain a professional, concise, and executive tone.

        === RETRIEVED CONTEXT ===
        %s
        === END OF CONTEXT ===
        """;

    public String buildSystemPrompt(List<RetrievedChunk> chunks) {
        if (chunks == null || chunks.isEmpty()) {
            return String.format(SYSTEM_PROMPT_TEMPLATE, "No relevant documents found.");
        }

        StringBuilder contextBuilder = new StringBuilder();
        for (int i = 0; i < chunks.size(); i++) {
            RetrievedChunk chunk = chunks.get(i);
            contextBuilder.append(String.format(
                    "--- [CHUNK %d] ---\n" +
                    "Document: %s\n" +
                    "Page Number: %d\n" +
                    "Chunk Index: %d\n" +
                    "Content:\n%s\n\n",
                    (i + 1),
                    chunk.getDocumentTitle(),
                    chunk.getPageNumber(),
                    chunk.getChunkIndex(),
                    chunk.getChunkText()
            ));
        }

        return String.format(SYSTEM_PROMPT_TEMPLATE, contextBuilder.toString().trim());
    }

    public String buildUserPrompt(String query) {
        return query != null ? query.trim() : "";
    }
}
