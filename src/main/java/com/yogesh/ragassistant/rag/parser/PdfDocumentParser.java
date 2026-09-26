package com.yogesh.ragassistant.rag.parser;

import com.yogesh.ragassistant.exception.DocumentProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class PdfDocumentParser implements DocumentParser {

    @Override
    public boolean supports(String fileExtension) {
        return "pdf".equalsIgnoreCase(fileExtension);
    }

    @Override
    public ParsedDocument parse(InputStream inputStream, String filename) {
        log.info("Parsing PDF document: {}", filename);
        List<ParsedDocument.ParsedPage> pages = new ArrayList<>();

        try {
            byte[] bytes = inputStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                int totalPages = document.getNumberOfPages();
                PDFTextStripper stripper = new PDFTextStripper();

                for (int pageNum = 1; pageNum <= totalPages; pageNum++) {
                    stripper.setStartPage(pageNum);
                    stripper.setEndPage(pageNum);
                    String pageText = stripper.getText(document);

                    String cleanedText = cleanText(pageText);
                    if (!cleanedText.isBlank()) {
                        pages.add(new ParsedDocument.ParsedPage(pageNum, cleanedText));
                    }
                }
            }
        } catch (Exception ex) {
            log.error("Failed to parse PDF document {}: {}", filename, ex.getMessage(), ex);
            throw new DocumentProcessingException("Error parsing PDF file: " + filename, ex);
        }

        return ParsedDocument.builder()
                .filename(filename)
                .pages(pages)
                .build();
    }

    private String cleanText(String text) {
        if (text == null) return "";
        // Remove null characters, carriage returns, and normalize multiple whitespace
        return text.replace("\u0000", "")
                .replace("\r\n", "\n")
                .replace("\r", "\n")
                .replaceAll("[ \\t]+", " ")
                .replaceAll("\n{3,}", "\n\n")
                .trim();
    }
}
