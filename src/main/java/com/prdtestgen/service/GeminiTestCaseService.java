package com.prdtestgen.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prdtestgen.model.TestPlanResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class GeminiTestCaseService {

    private static final String API_BASE = "https://generativelanguage.googleapis.com/v1beta/models/";

    private static final String SYSTEM_PROMPT = """
            You are a senior QA test engineer. Given a product requirements document (PRD) \
            or a screenshot of a product UI, produce a test plan and a list of concrete, \
            executable test cases covering: the golden/happy path, edge cases, negative/invalid-input \
            cases, and any UI or accessibility concerns visible in the material.
            """;

    private static final String RESPONSE_SCHEMA_JSON = """
            {
              "type": "object",
              "properties": {
                "summary": {"type": "string", "description": "One paragraph summary of what is being tested"},
                "scope": {"type": "string", "description": "What is in scope / out of scope for this test plan"},
                "assumptions": {"type": "array", "items": {"type": "string"}},
                "testCases": {
                  "type": "array",
                  "items": {
                    "type": "object",
                    "properties": {
                      "id": {"type": "string"},
                      "title": {"type": "string"},
                      "type": {
                        "type": "string",
                        "enum": ["Functional", "Negative", "Edge Case", "UI", "Regression", "Accessibility", "Performance"]
                      },
                      "priority": {"type": "string", "enum": ["High", "Medium", "Low"]},
                      "preconditions": {"type": "string"},
                      "steps": {"type": "array", "items": {"type": "string"}},
                      "expectedResult": {"type": "string"}
                    },
                    "required": ["id", "title", "type", "priority", "steps", "expectedResult"]
                  }
                }
              },
              "required": ["summary", "scope", "testCases"]
            }
            """;

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String apiKey;
    private final String model;
    private final long maxTokens;
    private final Object responseSchema;

    public GeminiTestCaseService(
            RestClient geminiRestClient,
            @Value("${GEMINI_API_KEY:}") String apiKey,
            @Value("${gemini.model:gemini-2.5-flash}") String model,
            @Value("${gemini.max-tokens:8192}") long maxTokens) {
        this.restClient = geminiRestClient;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
        try {
            this.responseSchema = objectMapper.readValue(RESPONSE_SCHEMA_JSON, Object.class);
        } catch (Exception e) {
            throw new IllegalStateException("Invalid built-in Gemini response schema", e);
        }
    }

    public TestPlanResult generateFromPrdText(String prdText) {
        Map<String, Object> textPart = Map.of("text", "Here is the PRD to analyze:\n\n" + prdText);
        return generate(List.of(textPart));
    }

    public TestPlanResult generateFromScreenshot(byte[] imageBytes, String mediaType, String extraInstructions) {
        String prompt = "Analyze this UI screenshot and generate test cases for it."
                + (StringUtils.hasText(extraInstructions) ? " " + extraInstructions : "");

        Map<String, Object> textPart = Map.of("text", prompt);
        Map<String, Object> imagePart = Map.of(
                "inline_data", Map.of(
                        "mime_type", normalizeMediaType(mediaType),
                        "data", Base64.getEncoder().encodeToString(imageBytes)));

        return generate(List.of(textPart, imagePart));
    }

    private TestPlanResult generate(List<Map<String, Object>> parts) {
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException(
                    "GEMINI_API_KEY is not set. Get a free key at https://aistudio.google.com/apikey "
                            + "and set it as an environment variable.");
        }

        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("contents", List.of(Map.of("role", "user", "parts", parts)));
        requestBody.put("systemInstruction", Map.of("parts", List.of(Map.of("text", SYSTEM_PROMPT))));
        requestBody.put("generationConfig", Map.of(
                "responseMimeType", "application/json",
                "responseSchema", responseSchema,
                "maxOutputTokens", maxTokens,
                "temperature", 0.2));

        String url = API_BASE + model + ":generateContent?key="
                + URLEncoder.encode(apiKey, StandardCharsets.UTF_8);

        String rawResponse = restClient.post()
                .uri(URI.create(url))
                .contentType(MediaType.APPLICATION_JSON)
                .body(requestBody)
                .retrieve()
                .body(String.class);

        return parse(rawResponse);
    }

    private TestPlanResult parse(String rawResponse) {
        JsonNode root;
        try {
            root = objectMapper.readTree(rawResponse);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Gemini returned a response that could not be parsed as JSON:\n" + rawResponse, e);
        }

        JsonNode blockReason = root.path("promptFeedback").path("blockReason");
        if (!blockReason.isMissingNode() && !blockReason.isNull()) {
            throw new IllegalStateException("Gemini blocked this request: " + blockReason.asText());
        }

        JsonNode textNode = root.path("candidates").path(0).path("content").path("parts").path(0).path("text");
        if (textNode.isMissingNode() || !textNode.isTextual()) {
            throw new IllegalStateException(
                    "Gemini did not return any generated content. Raw response:\n" + rawResponse);
        }

        try {
            return objectMapper.readValue(textNode.asText(), TestPlanResult.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Gemini did not return valid JSON for the test plan. Raw response:\n" + textNode.asText(), e);
        }
    }

    private String normalizeMediaType(String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "image/jpeg", "image/jpg" -> "image/jpeg";
            case "image/gif" -> "image/gif";
            case "image/webp" -> "image/webp";
            default -> "image/png";
        };
    }
}
