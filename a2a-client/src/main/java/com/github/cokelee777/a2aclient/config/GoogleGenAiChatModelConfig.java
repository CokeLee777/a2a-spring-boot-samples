package com.github.cokelee777.a2aclient.config;

import com.github.cokelee777.a2aclient.tools.A2aTools;
import com.google.genai.Client;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
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

import java.util.List;

@Configuration
@ConditionalOnProperty(name = "app.chat.provider", havingValue = "google-genai")
public class GoogleGenAiChatModelConfig {

	private static final String SYSTEM_PROMPT = """
			당신은 주문/배송 고객 지원 에이전트입니다.
			주문 취소 문의는 checkOrderCancellability 도구를,
			배송 조회 문의는 trackDelivery 도구를 사용하세요.
			""";

	@Bean
	public ChatModel chatModel(@Value("${spring.ai.google.genai.api-key}") String apiKey,
			@Value("${spring.ai.google.genai.chat.options.model}") String model,
			@Value("${spring.ai.google.genai.chat.options.temperature}") double temperature) {
		Client client = Client.builder().apiKey(apiKey).build();
		GoogleGenAiChatOptions options = GoogleGenAiChatOptions.builder().model(model).temperature(temperature).build();
		ToolCallingManager toolCallingManager = DefaultToolCallingManager.builder().build();
		RetryTemplate retryTemplate = new RetryTemplate();
		ObservationRegistry observationRegistry = ObservationRegistry.NOOP;
		return new GoogleGenAiChatModel(client, options, toolCallingManager, retryTemplate, observationRegistry);
	}

	@Bean
	public <T extends A2aTools> ChatClient chatClient(ChatModel chatModel, List<T> tools) {
		return ChatClient.builder(chatModel)
			.defaultSystem(SYSTEM_PROMPT)
			.defaultTools(tools.toArray())
			.defaultAdvisors(new SimpleLoggerAdvisor())
			.build();
	}

}
