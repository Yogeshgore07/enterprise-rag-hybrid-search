package com.yogesh.ragassistant.rag.parser;

import com.yogesh.ragassistant.exception.DocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class TxtDocumentParser implements DocumentParser {

    private static final int LINES_PER_PAGE = 50;

    @Override
    public boolean supports(String fileExtension) {
        return "txt".equalsIgnoreCase(fileExtension) || "md".equalsIgnoreCase(fileExtension);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) {
        log.info("Parsing TXT document: {}", filename);
        List<ParsedDocument.ParsedPage> pages = new ArrayList<>();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            StringBuilder currentContent = new StringBuilder();
            int lineCount = 0;
            int pageNumber = 1;

            while ((line = reader.readLine()) != null) {
                currentContent.append(line).append("\n");
                lineCount++;

                if (lineCount >= LINES_PER_PAGE) {
                    pages.add(new ParsedDocument.ParsedPage(pageNumber++, currentContent.toString().trim()));
                    currentContent.setLength(0);
                    lineCount = 0;
                }
            }

            if (currentContent.length() > 0) {
                pages.add(new ParsedDocument.ParsedPage(pageNumber, currentContent.toString().trim()));
            }

        } catch (Exception ex) {
            log.error("Failed to parse TXT document {}: {}", filename, ex.getMessage(), ex);
            throw new DocumentProcessingException("Error parsing TXT file: " + filename, ex);
        }

        return ParsedDocument.builder()
                .filename(filename)
                .pages(pages)
                .build();
    }
}
