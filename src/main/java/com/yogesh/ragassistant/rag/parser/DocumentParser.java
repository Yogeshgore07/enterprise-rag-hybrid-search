package com.yogesh.ragassistant.rag.parser;

import java.io.InputStream;

public interface DocumentParser {

    /**
     * Whether this parser supports the given file extension (e.g. "pdf", "docx", "txt", "csv").
     */
    boolean supports(String fileExtension);

    /**
     * Parse document stream into structured pages with page numbers.
     */
    ParsedDocument parse(InputStream inputStream, String filename);
}
