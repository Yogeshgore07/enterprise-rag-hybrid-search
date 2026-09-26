package com.yogesh.ragassistant.reranker;

import com.yogesh.ragassistant.retriever.RetrievedChunk;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service("noOpReranker")
@Slf4j
public class NoOpReranker implements RerankService {

    @Override
    public List<RetrievedChunk> rerank(String query, List<RetrievedChunk> candidates, int topN) {
        log.debug("NoOpReranker: preserving top {} hybrid candidates", topN);
        if (candidates == null || candidates.isEmpty()) {
            return List.of();
        }
        return candidates.stream().limit(topN).toList();
    }

    @Override
    public String getProviderName() {
        return "noop";
    }
}
