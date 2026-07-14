package com.prdtestgen.config;

import com.anthropic.client.AnthropicClient;
import com.anthropic.client.okhttp.AnthropicOkHttpClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class ClaudeConfig {

    /**
     * Reads credentials from the ANTHROPIC_API_KEY environment variable.
     * Never hardcode the key here or in application.properties.
     */
    @Bean
    public AnthropicClient anthropicClient() {
        return AnthropicOkHttpClient.fromEnv();
    }
}
