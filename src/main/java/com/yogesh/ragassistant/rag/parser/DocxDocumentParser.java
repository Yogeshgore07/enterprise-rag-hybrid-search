package com.yogesh.ragassistant.rag.parser;

import com.yogesh.ragassistant.exception.DocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.xwpf.usermodel.*;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class DocxDocumentParser implements DocumentParser {

    private static final int ESTIMATED_WORDS_PER_PAGE = 400;

    @Override
    public boolean supports(String fileExtension) {
        return "docx".equalsIgnoreCase(fileExtension) || "doc".equalsIgnoreCase(fileExtension);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) {
        log.info("Parsing DOCX document: {}", filename);
        List<ParsedDocument.ParsedPage> pages = new ArrayList<>();

        try (XWPFDocument document = new XWPFDocument(inputStream)) {
            StringBuilder currentContent = new StringBuilder();
            int currentPage = 1;
            int wordCount = 0;

            // Extract paragraphs
            for (XWPFParagraph paragraph : document.getParagraphs()) {
                String text = paragraph.getText();
                if (text == null || text.isBlank()) continue;

                currentContent.append(text).append("\n\n");
                wordCount += text.split("\\s+").length;

                if (wordCount >= ESTIMATED_WORDS_PER_PAGE) {
                    pages.add(new ParsedDocument.ParsedPage(currentPage++, currentContent.toString().trim()));
                    currentContent.setLength(0);
                    wordCount = 0;
                }
            }

            // Extract tables
            for (XWPFTable table : document.getTables()) {
                StringBuilder tableText = new StringBuilder("\n[TABLE DATA]\n");
                for (XWPFTableRow row : table.getRows()) {
                    List<String> cellTexts = new ArrayList<>();
                    for (XWPFTableCell cell : row.getTableCells()) {
                        cellTexts.add(cell.getText().trim());
                    }
                    tableText.append(String.join(" | ", cellTexts)).append("\n");
                }
                tableText.append("[END TABLE DATA]\n\n");
                currentContent.append(tableText);
            }

            if (currentContent.length() > 0) {
                pages.add(new ParsedDocument.ParsedPage(currentPage, currentContent.toString().trim()));
            }

        } catch (Exception ex) {
            log.error("Failed to parse DOCX document {}: {}", filename, ex.getMessage(), ex);
            throw new DocumentProcessingException("Error parsing DOCX file: " + filename, ex);
        }

        return ParsedDocument.builder()
                .filename(filename)
                .pages(pages)
                .build();
    }
}
