package com.github.cokelee777.a2aclient.config;

import com.google.genai.Client;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatModel;
import org.springframework.ai.google.genai.GoogleGenAiChatOptions;
import org.springframework.ai.model.tool.DefaultToolCallingManager;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.support.RetryTemplate;

@Configuration
@ConditionalOnProperty(name = "app.chat.provider", havingValue = "google-genai")
public class GoogleGenAiChatModelConfig {

    @Bean
    public ChatModel chatModel(
            @Value("${spring.ai.google.genai.api-key}") String apiKey,
            @Value("${spring.ai.google.genai.chat.options.model:gemini-2.5-flash-lite}") String model,
            @Value("${spring.ai.google.genai.chat.options.temperature:0.7}") double temperature) {
        Client client = Client.builder()
                .apiKey(apiKey)
                .build();
        GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .build();
        ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder().build();
        RetryTemplate retryTemplate = new RetryTemplate();
        ObservationRegistry observationRegistry = ObservationRegistry.NOOP;
        return new GoogleGenAiChatModel(client, options, toolCallingManager, retryTemplate, observationRegistry);
    }
}
