package com.prdtestgen.web;

import com.prdtestgen.model.HistorySummary;
import com.prdtestgen.model.TestPlanResult;
import com.prdtestgen.service.DocumentExtractionService;
import com.prdtestgen.service.GeminiTestCaseService;
import com.prdtestgen.service.GenerationHistoryService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api")
public class TestGenController {

    private static final Logger log = LoggerFactory.getLogger(TestGenController.class);
    private static final Pattern API_KEY_PATTERN = Pattern.compile("key=[^&\\s\"]+");

    private final DocumentExtractionService documentExtractionService;
    private final GeminiTestCaseService geminiTestCaseService;
    private final GenerationHistoryService generationHistoryService;

    public TestGenController(
            DocumentExtractionService documentExtractionService,
            GeminiTestCaseService geminiTestCaseService,
            GenerationHistoryService generationHistoryService) {
        this.documentExtractionService = documentExtractionService;
        this.geminiTestCaseService = geminiTestCaseService;
        this.generationHistoryService = generationHistoryService;
    }

    @PostMapping(value = "/prd/analyze", consumes = "multipart/form-data")
    public ResponseEntity<?> analyzePrd(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return badRequest("No file was uploaded.");
        }
        try {
            String prdText = documentExtractionService.extractText(file);
            if (prdText.isBlank()) {
                return badRequest("No extractable text was found in the uploaded document.");
            }
            TestPlanResult result = geminiTestCaseService.generateFromPrdText(prdText);
            saveHistory("PRD", file.getOriginalFilename(), result);
            return ResponseEntity.ok(result);
        } catch (IllegalArgumentException e) {
            return badRequest(e.getMessage());
        } catch (IOException e) {
            return badRequest("Could not read the uploaded file: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping(value = "/screenshot/analyze", consumes = "multipart/form-data")
    public ResponseEntity<?> analyzeScreenshot(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "prompt", required = false) String prompt) {
        if (file.isEmpty()) {
            return badRequest("No screenshot was uploaded.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            return badRequest("Uploaded file must be an image (PNG, JPEG, GIF, or WebP).");
        }
        try {
            TestPlanResult result =
                    geminiTestCaseService.generateFromScreenshot(file.getBytes(), contentType, prompt);
            saveHistory("SCREENSHOT", file.getOriginalFilename(), result);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return badRequest("Could not read the uploaded screenshot: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/history")
    public ResponseEntity<List<HistorySummary>> listHistory() {
        return ResponseEntity.ok(generationHistoryService.listSummaries());
    }

    @GetMapping("/history/{id}")
    public ResponseEntity<?> getHistoryItem(@PathVariable Long id) {
        return generationHistoryService.getFullResult(id)
                .<ResponseEntity<?>>map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    private void saveHistory(String sourceType, String sourceFilename, TestPlanResult result) {
        try {
            generationHistoryService.save(sourceType, sourceFilename, result);
        } catch (Exception e) {
            log.warn("Failed to save generation history for {} '{}': {}", sourceType, sourceFilename, e.getMessage());
        }
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(RestClientResponseException.class)
    public ResponseEntity<Map<String, String>> handleGeminiHttpError(RestClientResponseException e) {
        HttpStatusCode status = e.getStatusCode();
        return ResponseEntity.status(status)
                .body(Map.of("error", "Gemini API error: " + redactApiKey(e.getResponseBodyAsString())));
    }

    @ExceptionHandler(RestClientException.class)
    public ResponseEntity<Map<String, String>> handleGeminiIoError(RestClientException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Could not reach the Gemini API: " + redactApiKey(e.getMessage())));
    }

    private String redactApiKey(String text) {
        if (text == null) {
            return "";
        }
        return API_KEY_PATTERN.matcher(text).replaceAll("key=REDACTED");
    }
}
