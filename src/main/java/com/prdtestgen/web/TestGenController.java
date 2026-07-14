package com.prdtestgen.web;

import com.anthropic.errors.AnthropicIoException;
import com.anthropic.errors.AnthropicServiceException;
import com.prdtestgen.model.TestPlanResult;
import com.prdtestgen.service.ClaudeTestCaseService;
import com.prdtestgen.service.DocumentExtractionService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestGenController {

    private final DocumentExtractionService documentExtractionService;
    private final ClaudeTestCaseService claudeTestCaseService;

    public TestGenController(
            DocumentExtractionService documentExtractionService, ClaudeTestCaseService claudeTestCaseService) {
        this.documentExtractionService = documentExtractionService;
        this.claudeTestCaseService = claudeTestCaseService;
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
            TestPlanResult result = claudeTestCaseService.generateFromPrdText(prdText);
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
                    claudeTestCaseService.generateFromScreenshot(file.getBytes(), contentType, prompt);
            return ResponseEntity.ok(result);
        } catch (IOException e) {
            return badRequest("Could not read the uploaded screenshot: " + e.getMessage());
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        }
    }

    private ResponseEntity<Map<String, String>> badRequest(String message) {
        return ResponseEntity.badRequest().body(Map.of("error", message));
    }

    @ExceptionHandler(AnthropicServiceException.class)
    public ResponseEntity<Map<String, String>> handleAnthropicServiceException(AnthropicServiceException e) {
        HttpStatus status = HttpStatus.resolve(e.statusCode());
        return ResponseEntity.status(status != null ? status : HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Claude API error: " + e.getMessage()));
    }

    @ExceptionHandler(AnthropicIoException.class)
    public ResponseEntity<Map<String, String>> handleAnthropicIoException(AnthropicIoException e) {
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                .body(Map.of("error", "Could not reach the Claude API: " + e.getMessage()));
    }
}
