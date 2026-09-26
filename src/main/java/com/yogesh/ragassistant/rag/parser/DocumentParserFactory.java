package com.yogesh.ragassistant.rag.parser;

import com.yogesh.ragassistant.exception.DocumentProcessingException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DocumentParserFactory {

    private final List<DocumentParser> parsers;

    public DocumentParser getParser(String filename) {
        if (filename == null || !filename.contains(".")) {
            throw new DocumentProcessingException("Invalid filename or missing extension: " + filename);
        }

        String extension = filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();

        return parsers.stream()
                .filter(p -> p.supports(extension))
                .findFirst()
                .orElseThrow(() -> new DocumentProcessingException(
                        "Unsupported file format [." + extension + "]. Supported formats: PDF, DOCX, TXT, CSV."));
    }
}
