package com.yogesh.ragassistant.embedding;

import com.yogesh.ragassistant.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Service("mockEmbeddingService")
@RequiredArgsConstructor
@Slf4j
public class MockEmbeddingService implements EmbeddingService {

    private final RagProperties ragProperties;

    @Override
    public float[] generateEmbedding(String text) {
        int dim = getDimension();
        float[] vector = new float[dim];

        // Seed deterministically based on text SHA-256 hash
        long seed = hashTextToLong(text);
        Random random = new Random(seed);

        double norm = 0.0;
        for (int i = 0; i < dim; i++) {
            vector[i] = (float) (random.nextGaussian());
            norm += vector[i] * vector[i];
        }

        // Normalize vector to unit length
        norm = Math.sqrt(norm);
        if (norm > 0) {
            for (int i = 0; i < dim; i++) {
                vector[i] = (float) (vector[i] / norm);
            }
        }

        return vector;
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        log.debug("Generating {} mock embeddings", texts.size());
        List<float[]> results = new ArrayList<>(texts.size());
        for (String text : texts) {
            results.add(generateEmbedding(text));
        }
        return results;
    }

    @Override
    public int getDimension() {
        return ragProperties.getEmbedding().getDimension();
    }

    @Override
    public String getProviderName() {
        return "mock";
    }

    private long hashTextToLong(String text) {
        if (text == null) {
            return 42L;
        }
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(text.getBytes(StandardCharsets.UTF_8));
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFF);
            }
            return hash;
        } catch (NoSuchAlgorithmException e) {
            return text.hashCode();
        }
    }
}
