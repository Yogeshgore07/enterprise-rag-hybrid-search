package com.yogesh.ragassistant.rag.chunking;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.HashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChunkWithMetadata {

    private int chunkIndex;

    private int pageNumber;

    private String heading;

    private String documentTitle;

    private String text;

    private int tokenCount;

    @Builder.Default
    private Map<String, Object> additionalMetadata = new HashMap<>();
}
