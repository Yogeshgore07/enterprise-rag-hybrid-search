package com.yogesh.ragassistant.rag.parser;

import com.opencsv.CSVReader;
import com.yogesh.ragassistant.exception.DocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class CsvDocumentParser implements DocumentParser {

    private static final int ROWS_PER_PAGE = 30;

    @Override
    public boolean supports(String fileExtension) {
        return "csv".equalsIgnoreCase(fileExtension) || "tsv".equalsIgnoreCase(fileExtension);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) {
        log.info("Parsing CSV document: {}", filename);
        List<ParsedDocument.ParsedPage> pages = new ArrayList<>();

        try (CSVReader reader = new CSVReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String[] headers = reader.readNext();
            if (headers == null || headers.length == 0) {
                return ParsedDocument.builder().filename(filename).pages(List.of()).build();
            }

            // Clean headers
            for (int i = 0; i < headers.length; i++) {
                headers[i] = headers[i].trim();
            }

            String[] row;
            StringBuilder currentContent = new StringBuilder();
            int rowCount = 0;
            int pageNumber = 1;

            while ((row = reader.readNext()) != null) {
                StringBuilder rowText = new StringBuilder();
                for (int i = 0; i < Math.min(headers.length, row.length); i++) {
                    String value = row[i].trim();
                    if (!value.isEmpty()) {
                        rowText.append(headers[i]).append(": ").append(value).append(", ");
                    }
                }
                if (rowText.length() > 2) {
                    rowText.setLength(rowText.length() - 2);
                    currentContent.append(rowText).append("\n");
                    rowCount++;
                }

                if (rowCount >= ROWS_PER_PAGE) {
                    pages.add(new ParsedDocument.ParsedPage(pageNumber++, currentContent.toString().trim()));
                    currentContent.setLength(0);
                    rowCount = 0;
                }
            }

            if (currentContent.length() > 0) {
                pages.add(new ParsedDocument.ParsedPage(pageNumber, currentContent.toString().trim()));
            }

        } catch (Exception ex) {
            log.error("Failed to parse CSV document {}: {}", filename, ex.getMessage(), ex);
            throw new DocumentProcessingException("Error parsing CSV file: " + filename, ex);
        }

        return ParsedDocument.builder()
                .filename(filename)
                .pages(pages)
                .build();
    }
}
