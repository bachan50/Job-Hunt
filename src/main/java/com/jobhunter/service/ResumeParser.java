package com.jobhunter.service;

import java.io.IOException;
import java.io.InputStream;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

/** Extracts plain text from an uploaded resume (PDF, DOCX or TXT). */
@Component
public class ResumeParser {

    public String extractText(MultipartFile file) throws IOException {
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();

        try (InputStream in = file.getInputStream()) {
            if (name.endsWith(".pdf") || contentType.contains("pdf")) {
                return extractPdf(in);
            }
            if (name.endsWith(".docx") || contentType.contains("word")
                    || contentType.contains("officedocument")) {
                return extractDocx(in);
            }
            // Fallback: treat as plain text.
            return new String(in.readAllBytes());
        }
    }

    private String extractPdf(InputStream in) throws IOException {
        try (PDDocument doc = PDDocument.load(in)) {
            return new PDFTextStripper().getText(doc);
        }
    }

    private String extractDocx(InputStream in) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(in);
             XWPFWordExtractor extractor = new XWPFWordExtractor(doc)) {
            return extractor.getText();
        }
    }
}
