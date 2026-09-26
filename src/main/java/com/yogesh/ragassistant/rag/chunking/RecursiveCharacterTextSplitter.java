package com.yogesh.ragassistant.rag.chunking;

import com.yogesh.ragassistant.config.RagProperties;
import com.yogesh.ragassistant.util.TokenUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * Production Recursive Character Text Splitter.
 * 
 * Recursively splits text along semantic boundaries (paragraphs, sentences, words, characters)
 * keeping token count below chunkSize while maintaining chunkOverlap for context continuity.
 */
@Component
@Slf4j
public class RecursiveCharacterTextSplitter {

    private static final Pattern HEADING_PATTERN = Pattern.compile("^(#{1,6}\\s+.*|[A-Z0-9\\s]{4,}:?|Chapter\\s+\\d+.*|Section\\s+\\d+.*)$", Pattern.MULTILINE);

    private final int chunkSize;
    private final int chunkOverlap;
    private final List<String> separators;

    public RecursiveCharacterTextSplitter() {
        this(500, 50, List.of("\n\n", "\n", ". ", "? ", "! ", "; ", " ", ""));
    }

    @Autowired
    public RecursiveCharacterTextSplitter(RagProperties ragProperties) {
        RagProperties.Chunking chunkConfig = ragProperties.getChunking();
        this.chunkSize = chunkConfig.getChunkSize();
        this.chunkOverlap = chunkConfig.getChunkOverlap();
        this.separators = chunkConfig.getSeparators() != null && !chunkConfig.getSeparators().isEmpty()
                ? chunkConfig.getSeparators()
                : List.of("\n\n", "\n", ". ", "? ", "! ", "; ", " ", "");
    }

    public RecursiveCharacterTextSplitter(int chunkSize, int chunkOverlap, List<String> separators) {
        this.chunkSize = chunkSize;
        this.chunkOverlap = chunkOverlap;
        this.separators = separators != null ? separators : List.of("\n\n", "\n", ". ", "? ", "! ", "; ", " ", "");
    }

    /**
     * Splits text into metadata-enriched chunks.
     */
    public List<ChunkWithMetadata> splitTextWithMetadata(String text, String documentTitle, int pageNumber, int startingChunkIndex) {
        if (text == null || text.isBlank()) {
            return List.of();
        }

        List<String> rawChunks = splitText(text);
        List<ChunkWithMetadata> result = new ArrayList<>(rawChunks.size());

        String currentHeading = extractFirstHeading(text);
        int chunkIdx = startingChunkIndex;

        for (String rawChunk : rawChunks) {
            String detectedHeading = extractFirstHeading(rawChunk);
            if (detectedHeading != null && !detectedHeading.isBlank()) {
                currentHeading = detectedHeading;
            }

            int tokens = TokenUtils.estimateTokenCount(rawChunk);
            ChunkWithMetadata chunk = ChunkWithMetadata.builder()
                    .chunkIndex(chunkIdx++)
                    .pageNumber(pageNumber)
                    .documentTitle(documentTitle)
                    .heading(currentHeading != null ? currentHeading : "General")
                    .text(rawChunk.trim())
                    .tokenCount(tokens)
                    .build();

            result.add(chunk);
        }

        return result;
    }

    /**
     * Core recursive splitting logic.
     */
    public List<String> splitText(String text) {
        return splitTextRecursively(text, separators);
    }

    private List<String> splitTextRecursively(String text, List<String> availableSeparators) {
        List<String> finalChunks = new ArrayList<>();
        int textTokens = TokenUtils.estimateTokenCount(text);

        if (textTokens <= chunkSize) {
            if (!text.trim().isEmpty()) {
                finalChunks.add(text.trim());
            }
            return finalChunks;
        }

        // Find appropriate separator
        String chosenSeparator = "";
        List<String> nextSeparators = List.of();

        for (int i = 0; i < availableSeparators.size(); i++) {
            String sep = availableSeparators.get(i);
            if (sep.isEmpty() || text.contains(sep)) {
                chosenSeparator = sep;
                nextSeparators = availableSeparators.subList(i + 1, availableSeparators.size());
                break;
            }
        }

        List<String> splits = splitOnSeparator(text, chosenSeparator);
        List<String> goodSplits = new ArrayList<>();

        for (String split : splits) {
            if (TokenUtils.estimateTokenCount(split) <= chunkSize) {
                goodSplits.add(split);
            } else if (!nextSeparators.isEmpty()) {
                // Recursively split oversized segment
                List<String> subChunks = splitTextRecursively(split, nextSeparators);
                goodSplits.addAll(subChunks);
            } else {
                // Hard character split fallback
                goodSplits.addAll(hardCharacterSplit(split, chunkSize));
            }
        }

        // Merge pieces back up to chunkSize with overlap
        return mergeSplits(goodSplits, chosenSeparator);
    }

    private List<String> splitOnSeparator(String text, String separator) {
        if (separator.isEmpty()) {
            List<String> chars = new ArrayList<>(text.length());
            for (char c : text.toCharArray()) {
                chars.add(String.valueOf(c));
            }
            return chars;
        }
        return List.of(text.split(Pattern.quote(separator)));
    }

    private List<String> mergeSplits(List<String> splits, String separator) {
        List<String> mergedChunks = new ArrayList<>();
        List<String> currentChunkParts = new ArrayList<>();
        int currentTokens = 0;

        for (String piece : splits) {
            if (piece.isBlank()) continue;
            int pieceTokens = TokenUtils.estimateTokenCount(piece);

            if (currentTokens + pieceTokens > chunkSize && !currentChunkParts.isEmpty()) {
                String doc = String.join(separator, currentChunkParts).trim();
                if (!doc.isEmpty()) {
                    mergedChunks.add(doc);
                }

                // Handle overlap: retain trailing parts that fit within overlap limit
                while (!currentChunkParts.isEmpty() && currentTokens > chunkOverlap) {
                    String removed = currentChunkParts.remove(0);
                    currentTokens -= TokenUtils.estimateTokenCount(removed);
                }
            }

            currentChunkParts.add(piece);
            currentTokens += pieceTokens;
        }

        if (!currentChunkParts.isEmpty()) {
            String doc = String.join(separator, currentChunkParts).trim();
            if (!doc.isEmpty()) {
                mergedChunks.add(doc);
            }
        }

        return mergedChunks;
    }

    private List<String> hardCharacterSplit(String text, int maxTokens) {
        int maxChars = TokenUtils.tokensToCharCount(maxTokens);
        List<String> chunks = new ArrayList<>();
        int start = 0;
        int step = Math.max(1, maxChars - TokenUtils.tokensToCharCount(chunkOverlap));

        while (start < text.length()) {
            int end = Math.min(start + maxChars, text.length());
            chunks.add(text.substring(start, end));
            start += step;
        }
        return chunks;
    }

    private String extractFirstHeading(String text) {
        if (text == null) return null;
        var matcher = HEADING_PATTERN.matcher(text);
        if (matcher.find()) {
            return matcher.group().replaceAll("^#+\\s*", "").trim();
        }
        return null;
    }
}
