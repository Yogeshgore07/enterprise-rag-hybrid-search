package com.yogesh.ragassistant.util;

/**
 * Token estimation utilities for text splitting and chunk sizing.
 * Standard heuristic: 1 token ≈ 4 characters in English text.
 */
public final class TokenUtils {

    private static final double CHARACTERS_PER_TOKEN = 4.0;

    private TokenUtils() {}

    /**
     * Estimates token count of a given string.
     */
    public static int estimateTokenCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return (int) Math.ceil(text.length() / CHARACTERS_PER_TOKEN);
    }

    /**
     * Converts a token count to approximate character count.
     */
    public static int tokensToCharCount(int tokens) {
        return (int) (tokens * CHARACTERS_PER_TOKEN);
    }
}
