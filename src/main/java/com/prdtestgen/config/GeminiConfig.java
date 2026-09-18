package com.prdtestgen.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class GeminiConfig {

    /**
     * Plain HTTP client for the Gemini REST API. The API key is read per-request
     * from the GEMINI_API_KEY environment variable by GeminiTestCaseService —
     * never hardcode it here.
     */
    @Bean
    public RestClient geminiRestClient() {
        return RestClient.create();
    }
}
