package com.yogesh.ragassistant.reranker;

import com.yogesh.ragassistant.config.RagProperties;
import com.yogesh.ragassistant.retriever.RetrievedChunk;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@Primary
@RequiredArgsConstructor
public class RerankServiceFactory implements RerankService {

    private final RagProperties ragProperties;
    private final NoOpReranker noOpReranker;
    private final BgeReranker bgeReranker;

    public RerankService getActiveService() {
        if (!ragProperties.getReranker().isEnabled()) {
            return noOpReranker;
        }
        String provider = ragProperties.getReranker().getProvider();
        if ("bge".equalsIgnoreCase(provider)) {
            return bgeReranker;
        }
        return noOpReranker;
    }

    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN) {
        return getActiveService().rerank(query, candidates, topN);
    }

    @Override
    public String getProviderName() {
        return getActiveService().getProviderName();
    }
}
