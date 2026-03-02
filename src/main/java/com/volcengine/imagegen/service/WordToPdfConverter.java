package com.volcengine.imagegen.service;

import com.volcengine.imagegen.model.WordDocumentContent;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Service for converting Word documents to PDF
 */
@Slf4j
@Service
public class WordToPdfConverter {

    /**
     * Extract content from Word document
     *
     * @param wordFile Word file (doc or docx)
     * @return Extracted content
     * @throws IOException if extraction fails
     */
    public WordDocumentContent extractContent(MultipartFile wordFile) throws IOException {
        String filename = wordFile.getOriginalFilename();
        String text;

        log.info("Extracting content from Word document: {}", filename);

        if (filename != null && filename.toLowerCase().endsWith(".docx")) {
            // .docx format (Office 2007+)
            try (XWPFDocument doc = new XWPFDocument(wordFile.getInputStream())) {
                XWPFWordExtractor extractor = new XWPFWordExtractor(doc);
                text = extractor.getText();
            }
        } else if (filename != null && filename.toLowerCase().endsWith(".doc")) {
            // .doc format (older Office)
            try (HWPFDocument doc = new HWPFDocument(wordFile.getInputStream())) {
                WordExtractor extractor = new WordExtractor(doc);
                text = extractor.getText();
            }
        } else {
            throw new IllegalArgumentException("Unsupported file format: " + filename);
        }

        log.info("Extracted {} characters from Word document", text.length());
        return new WordDocumentContent(text, filename);
    }

    /**
     * Convert Word document to PDF using text content
     * Note: This creates a simple PDF with text content, formatting may be lost
     *
     * @param wordFile Word file (doc or docx)
     * @return PDF file
     * @throws IOException if conversion fails
     */
    public File convertToPdf(MultipartFile wordFile) throws IOException {
        log.info("Converting Word to PDF using Apache POI and PDFBox");

        // Extract text content
        WordDocumentContent content = extractContent(wordFile);

        // Clean up text - replace tabs and other control characters that fonts may not support
        String cleanText = content.text()
                .replace("\t", "    ")  // Replace tabs with spaces
                .replace("\r", "");     // Remove carriage returns

        // Split text into lines and add to PDF
        String[] paragraphs = cleanText.split("\n");

        // Create PDF
        Path tempPath = Files.createTempFile("converted_", ".pdf");
        File pdfFile = tempPath.toFile();

        try (PDDocument document = new PDDocument()) {
            // Load Chinese font - try system fonts, fallback to bundled font
            PDFont chineseFont = loadChineseFont(document);

            PDPage page = new PDPage();
            document.addPage(page);

            PDPageContentStream contentStream = new PDPageContentStream(document, page);
            contentStream.beginText();
            contentStream.setFont(chineseFont, 12);
            contentStream.newLineAtOffset(50, 750);

            float yPosition = 750;

            for (String paragraph : paragraphs) {
                // Handle long lines by wrapping (split by character for CJK text)
                String line = "";
                float lineWidth = 0;
                float maxLineWidth = 500; // Maximum line width in points

                for (int i = 0; i < paragraph.length(); i++) {
                    String c = String.valueOf(paragraph.charAt(i));
                    float charWidth = chineseFont.getStringWidth(c) / 1000 * 12;

                    if (lineWidth + charWidth > maxLineWidth && line.length() > 0) {
                        // Add line to PDF
                        contentStream.showText(line);
                        contentStream.newLineAtOffset(0, -15);
                        yPosition -= 15;

                        // New page if needed
                        if (yPosition < 50) {
                            contentStream.endText();
                            contentStream.close();
                            PDPage newPage = new PDPage();
                            document.addPage(newPage);
                            contentStream = new PDPageContentStream(document, newPage);
                            contentStream.beginText();
                            contentStream.setFont(chineseFont, 12);
                            contentStream.newLineAtOffset(50, 750);
                            yPosition = 750;
                        }

                        line = c;
                        lineWidth = charWidth;
                    } else {
                        line += c;
                        lineWidth += charWidth;
                    }
                }

                if (line.length() > 0) {
                    contentStream.showText(line);
                    contentStream.newLineAtOffset(0, -15);
                    yPosition -= 15;

                    if (yPosition < 50) {
                        contentStream.endText();
                        contentStream.close();
                        PDPage newPage = new PDPage();
                        document.addPage(newPage);
                        contentStream = new PDPageContentStream(document, newPage);
                        contentStream.beginText();
                        contentStream.setFont(chineseFont, 12);
                        contentStream.newLineAtOffset(50, 750);
                        yPosition = 750;
                    }
                }
            }

            contentStream.endText();
            contentStream.close();

            document.save(pdfFile);
            log.info("PDF created successfully: {}", pdfFile.getPath());
        }

        return pdfFile;
    }

    /**
     * Load a Chinese-compatible TrueType font
     * Tries common system fonts, falls back to PDFBox built-in
     */
    private PDFont loadChineseFont(PDDocument document) throws IOException {
        // Common Chinese font paths for different OS
        String[] fontPaths = {
            // macOS
            "/System/Library/Fonts/PingFang.ttc",
            "/System/Library/Fonts/STHeiti Light.ttc",
            "/Library/Fonts/Arial Unicode.ttf",
            // Linux
            "/usr/share/fonts/truetype/noto/NotoSansCJK-Regular.ttc",
            "/usr/share/fonts/truetype/wqy/wqy-zenhei.ttc",
            "/usr/share/fonts/opentype/noto/NotoSansCJK-Regular.ttc",
            // Windows
            "C:/Windows/Fonts/msyh.ttc",
            "C:/Windows/Fonts/simsun.ttc",
            "C:/Windows/Fonts/simhei.ttf",
        };

        // Try to load a system font
        for (String fontPath : fontPaths) {
            try {
                java.io.File fontFile = new java.io.File(fontPath);
                if (fontFile.exists()) {
                    log.info("Loading Chinese font from: {}", fontPath);
                    return PDType0Font.load(document, fontFile);
                }
            } catch (Exception e) {
                // Continue to next font
            }
        }

        // Fallback: try loading from resources
        try {
            java.io.InputStream fontStream = getClass().getResourceAsStream("/fonts/NotoSansSC-Regular.ttf");
            if (fontStream != null) {
                log.info("Loading Chinese font from resources");
                return PDType0Font.load(document, fontStream);
            }
        } catch (Exception e) {
            // Continue to fallback
        }

        // Last resort - use a basic font (won't support Chinese, but won't crash)
        log.warn("No Chinese font found, falling back to basic font. Chinese characters may not display correctly.");
        return org.apache.pdfbox.pdmodel.font.PDType1Font.HELVETICA;
    }

    /**
     * Check if file is a Word document
     */
    public boolean isWordDocument(String filename) {
        if (filename == null) return false;
        String lowerName = filename.toLowerCase();
        return lowerName.endsWith(".doc") || lowerName.endsWith(".docx");
    }

    /**
     * Check if file is a PDF
     */
    public boolean isPdfDocument(String filename) {
        if (filename == null) return false;
        return filename.toLowerCase().endsWith(".pdf");
    }
}
