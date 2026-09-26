package com.yogesh.ragassistant.rag.llm;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("mockLlmService")
@Slf4j
public class MockLlmService implements LlmService {

    private static final Pattern CHUNK_PATTERN = Pattern.compile(
            "--- \\[CHUNK \\d+\\] ---\\s*\\nDocument:\\s*(.+?)\\s*\\nPage Number:\\s*(\\d+)\\s*\\nChunk Index:\\s*(\\d+)\\s*\\nContent:\\s*\\n([\\s\\S]*?)(?=\\n--- \\[CHUNK|\\n=== END OF CONTEXT|$)"
    );

    @Override
    public String generateAnswer(String systemPrompt, String userPrompt) {
        log.info("Generating mock grounded LLM response for query: '{}'", userPrompt);

        // Check if context contains any documents
        if (systemPrompt == null || systemPrompt.contains("No relevant documents found") || !systemPrompt.contains("[CHUNK")) {
            return "I cannot answer this question based on the provided documents because no relevant context was found.";
        }

        Matcher matcher = CHUNK_PATTERN.matcher(systemPrompt);
        if (matcher.find()) {
            String docTitle = matcher.group(1).trim();
            String pageNum = matcher.group(2).trim();
            String chunkIdx = matcher.group(3).trim();
            String content = matcher.group(4).trim();

            String excerpt = content.length() > 300 ? content.substring(0, 300) + "..." : content;

            return "Based on the retrieved excerpts from '" + docTitle + "', here is the relevant information for '" + userPrompt + "':\n\n" +
                    excerpt + "\n\n" +
                    "[Doc: " + docTitle + ", Page: " + pageNum + ", Chunk: " + chunkIdx + "]";
        }

        // Return a grounded mock response demonstrating citation behavior
        return "Based on the enterprise documents provided, the information requested about '" + userPrompt +
                "' is documented in the knowledge repository.\n\n" +
                "According to the source documentation, key operational processes and guidelines are outlined " +
                "in detail to ensure compliance across departments [Doc: Document, Page: 1, Chunk: 0].";
    }

    @Override
    public String getProviderName() {
        return "mock";
    }
}
