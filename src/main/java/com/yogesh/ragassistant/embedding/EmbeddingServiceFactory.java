package com.yogesh.ragassistant.embedding;

import com.yogesh.ragassistant.config.RagProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
@RequiredArgsConstructor
@Slf4j
public class EmbeddingServiceFactory implements EmbeddingService {

    private final RagProperties ragProperties;
    private final MockEmbeddingService mockEmbeddingService;

    public EmbeddingService getActiveService() {
        return mockEmbeddingService;
    }

    @Override
    public float[] generateEmbedding(String text) {
        return getActiveService().generateEmbedding(text);
    }

    @Override
    public List<float[]> generateEmbeddings(List<String> texts) {
        return getActiveService().generateEmbeddings(texts);
    }

    @Override
    public int getDimension() {
        return getActiveService().getDimension();
    }

    @Override
    public String getProviderName() {
        return getActiveService().getProviderName();
    }
}
