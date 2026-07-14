package com.prdtestgen.service;

import com.anthropic.client.AnthropicClient;
import com.anthropic.models.messages.Base64ImageSource;
import com.anthropic.models.messages.ContentBlockParam;
import com.anthropic.models.messages.ImageBlockParam;
import com.anthropic.models.messages.Message;
import com.anthropic.models.messages.MessageCreateParams;
import com.anthropic.models.messages.Model;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.prdtestgen.model.TestPlanResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class ClaudeTestCaseService {

    private static final Pattern CODE_FENCE = Pattern.compile("```(?:json)?\\s*([\\s\\S]*?)\\s*```");

    private static final String SYSTEM_PROMPT = """
            You are a senior QA test engineer. Given a product requirements document (PRD) \
            or a screenshot of a product UI, produce a test plan and a list of concrete, \
            executable test cases covering: the golden/happy path, edge cases, negative/invalid-input \
            cases, and any UI or accessibility concerns visible in the material.

            Respond with ONLY a single JSON object matching this exact schema — no markdown code \
            fences, no commentary before or after the JSON:

            {
              "summary": "one paragraph summary of what is being tested",
              "scope": "what is in scope / out of scope for this test plan",
              "assumptions": ["assumption 1", "assumption 2"],
              "testCases": [
                {
                  "id": "TC-001",
                  "title": "short descriptive title",
                  "type": "Functional | Negative | Edge Case | UI | Regression | Accessibility | Performance",
                  "priority": "High | Medium | Low",
                  "preconditions": "state required before running this test",
                  "steps": ["step 1", "step 2"],
                  "expectedResult": "what should happen if the test passes"
                }
              ]
            }
            """;

    private final AnthropicClient client;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String model;
    private final long maxTokens;

    public ClaudeTestCaseService(
            AnthropicClient client,
            @Value("${anthropic.model:claude-opus-4-8}") String model,
            @Value("${anthropic.max-tokens:8192}") long maxTokens) {
        this.client = client;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    public TestPlanResult generateFromPrdText(String prdText) {
        String userPrompt = "Here is the PRD to analyze:\n\n" + prdText;

        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.of(model))
                .maxTokens(maxTokens)
                .system(SYSTEM_PROMPT)
                .addUserMessage(userPrompt)
                .build();

        Message message = client.messages().create(params);
        return parse(extractText(message));
    }

    public TestPlanResult generateFromScreenshot(byte[] imageBytes, String mediaType, String extraInstructions) {
        String userPrompt = "Analyze this UI screenshot and generate test cases for it."
                + (extraInstructions == null || extraInstructions.isBlank() ? "" : " " + extraInstructions);

        String base64Image = java.util.Base64.getEncoder().encodeToString(imageBytes);

        ContentBlockParam imageBlock = ContentBlockParam.ofImage(ImageBlockParam.builder()
                .source(Base64ImageSource.builder()
                        .mediaType(toMediaType(mediaType))
                        .data(base64Image)
                        .build())
                .build());

        MessageCreateParams params = MessageCreateParams.builder()
                .model(Model.of(model))
                .maxTokens(maxTokens)
                .system(SYSTEM_PROMPT)
                .addUserMessage(userPrompt)
                .addUserMessageOfBlockParams(List.of(imageBlock))
                .build();

        Message message = client.messages().create(params);
        return parse(extractText(message));
    }

    private Base64ImageSource.MediaType toMediaType(String contentType) {
        String normalized = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "image/jpeg", "image/jpg" -> Base64ImageSource.MediaType.IMAGE_JPEG;
            case "image/gif" -> Base64ImageSource.MediaType.IMAGE_GIF;
            case "image/webp" -> Base64ImageSource.MediaType.IMAGE_WEBP;
            default -> Base64ImageSource.MediaType.IMAGE_PNG;
        };
    }

    private String extractText(Message message) {
        return message.content().stream()
                .flatMap(contentBlock -> contentBlock.text().stream())
                .map(textBlock -> textBlock.text())
                .reduce("", String::concat);
    }

    private TestPlanResult parse(String rawResponse) {
        String json = stripCodeFence(rawResponse.trim());
        try {
            return objectMapper.readValue(json, TestPlanResult.class);
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Claude did not return valid JSON for the test plan. Raw response:\n" + rawResponse, e);
        }
    }

    private String stripCodeFence(String text) {
        Matcher matcher = CODE_FENCE.matcher(text);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        return text;
    }
}
