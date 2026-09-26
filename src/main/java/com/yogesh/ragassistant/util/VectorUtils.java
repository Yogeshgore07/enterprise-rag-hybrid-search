package com.yogesh.ragassistant.util;

import java.util.Arrays;
import java.util.List;

/**
 * Utility functions for vector transformations, pgvector format conversion,
 * cosine similarity computation, and normalization.
 */
public final class VectorUtils {

    private VectorUtils() {}

    /**
     * Converts a float[] array into PostgreSQL pgvector text format: "[0.123,0.456,...]"
     */
    public static String toPgVectorString(float[] vector) {
        if (vector == null || vector.length == 0) {
            return "[]";
        }
        StringBuilder sb = new StringBuilder(vector.length * 8);
        sb.append('[');
        for (int i = 0; i < vector.length; i++) {
            sb.append(vector[i]);
            if (i < vector.length - 1) {
                sb.append(',');
            }
        }
        sb.append(']');
        return sb.toString();
    }

    /**
     * Parses a pgvector text format string "[0.123,0.456,...]" to float[].
     */
    public static float[] fromPgVectorString(String vectorStr) {
        if (vectorStr == null || vectorStr.isBlank()) {
            return new float[0];
        }
        String clean = vectorStr.trim();
        if (clean.startsWith("[") && clean.endsWith("]")) {
            clean = clean.substring(1, clean.length() - 1);
        }
        if (clean.isBlank()) {
            return new float[0];
        }
        String[] parts = clean.split(",");
        float[] result = new float[parts.length];
        for (int i = 0; i < parts.length; i++) {
            result[i] = Float.parseFloat(parts[i].trim());
        }
        return result;
    }

    /**
     * Computes cosine similarity between two float vectors.
     */
    public static double cosineSimilarity(float[] vectorA, float[] vectorB) {
        if (vectorA == null || vectorB == null || vectorA.length != vectorB.length) {
            return 0.0;
        }
        double dotProduct = 0.0;
        double normA = 0.0;
        double normB = 0.0;
        for (int i = 0; i < vectorA.length; i++) {
            dotProduct += vectorA[i] * vectorB[i];
            normA += vectorA[i] * vectorA[i];
            normB += vectorB[i] * vectorB[i];
        }
        if (normA <= 0.0 || normB <= 0.0) {
            return 0.0;
        }
        return dotProduct / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    /**
     * Min-Max Normalizes a list of scores into range [0.0, 1.0].
     */
    public static double[] minMaxNormalize(double[] scores) {
        if (scores == null || scores.length == 0) {
            return new double[0];
        }
        double min = Arrays.stream(scores).min().orElse(0.0);
        double max = Arrays.stream(scores).max().orElse(1.0);

        if (Double.compare(min, max) == 0) {
            double[] normalized = new double[scores.length];
            Arrays.fill(normalized, 1.0);
            return normalized;
        }

        double[] normalized = new double[scores.length];
        for (int i = 0; i < scores.length; i++) {
            normalized[i] = (scores[i] - min) / (max - min);
        }
        return normalized;
    }
}
