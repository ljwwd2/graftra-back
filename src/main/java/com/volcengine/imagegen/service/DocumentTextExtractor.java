package com.volcengine.imagegen.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

/**
 * Service for extracting text from various document formats
 */
@Slf4j
@Service
public class DocumentTextExtractor {

    /**
     * Extract text from a document file
     *
     * @param file The document file
     * @return Extracted text content
     * @throws IOException if file reading fails
     */
    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename();
        if (filename == null) {
            throw new IllegalArgumentException("Invalid file name");
        }

        String extension = getFileExtension(filename).toLowerCase();

        return switch (extension) {
            case "pdf" -> extractFromPdf(file.getInputStream());
            case "doc", "docx" -> extractFromWord(file.getInputStream(), extension);
            case "txt" -> extractFromText(file.getInputStream());
            default -> throw new IllegalArgumentException("Unsupported file format: " + extension);
        };
    }

    /**
     * Extract text from PDF file
     */
    private String extractFromPdf(InputStream inputStream) throws IOException {
        try (PDDocument document = PDDocument.load(inputStream)) {
            org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
            return stripper.getText(document);
        }
    }

    /**
     * Extract text from Word document
     */
    private String extractFromWord(InputStream inputStream, String extension) throws IOException {
        if ("doc".equals(extension)) {
            try (HWPFDocument document = new HWPFDocument(inputStream)) {
                WordExtractor extractor = new WordExtractor(document);
                return extractor.getText();
            }
        } else {
            try (XWPFDocument document = new XWPFDocument(inputStream)) {
                StringBuilder text = new StringBuilder();
                List<XWPFParagraph> paragraphs = document.getParagraphs();
                for (XWPFParagraph paragraph : paragraphs) {
                    text.append(paragraph.getText()).append("\n");
                }
                return text.toString();
            }
        }
    }

    /**
     * Extract text from plain text file
     */
    private String extractFromText(InputStream inputStream) throws IOException {
        StringBuilder text = new StringBuilder();
        byte[] buffer = new byte[1024];
        int bytesRead;
        while ((bytesRead = inputStream.read(buffer)) != -1) {
            text.append(new String(buffer, 0, bytesRead));
        }
        return text.toString();
    }

    /**
     * Get file extension from filename
     */
    private String getFileExtension(String filename) {
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0 && lastDot < filename.length() - 1) {
            return filename.substring(lastDot + 1);
        }
        return "";
    }
}
