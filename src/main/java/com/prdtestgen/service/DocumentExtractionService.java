package com.prdtestgen.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Locale;

@Service
public class DocumentExtractionService {

    public String extractText(MultipartFile file) throws IOException {
        String filename = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase(Locale.ROOT);

        if (filename.endsWith(".pdf")) {
            return extractPdf(file);
        }
        if (filename.endsWith(".docx")) {
            return extractDocx(file);
        }
        if (filename.endsWith(".txt") || filename.endsWith(".md")) {
            return new String(file.getBytes());
        }
        throw new IllegalArgumentException(
                "Unsupported file type: " + filename + ". Upload a .pdf, .docx, .txt, or .md file.");
    }

    private String extractPdf(MultipartFile file) throws IOException {
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    private String extractDocx(MultipartFile file) throws IOException {
        try (XWPFDocument document = new XWPFDocument(file.getInputStream());
                XWPFWordExtractor extractor = new XWPFWordExtractor(document)) {
            return extractor.getText();
        }
    }
}
