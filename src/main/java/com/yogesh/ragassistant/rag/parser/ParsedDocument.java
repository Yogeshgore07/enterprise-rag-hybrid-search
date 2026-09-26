package com.yogesh.ragassistant.rag.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParsedDocument {

    private String filename;

    @Builder.Default
    private List<ParsedPage> pages = new ArrayList<>();

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ParsedPage {
        private int pageNumber;
        private String content;
    }
}
