package com.github.cokelee777.a2aclient.config;

import com.github.cokelee777.a2aclient.tools.A2aTool;
import com.google.genai.Client;
import io.micrometer.observation.ObservationRegistry;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.MessageChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
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

/**
 * Configuration for the Google GenAI chat model provider.
 * <p>
 * This configuration is conditionally loaded only when {@code app.chat.provider} is set
 * to "google-genai". It creates a {@code ChatModel} instance that uses Google's Gemini
 * LLM and configures a {@code ChatClient} with predefined tools and conversation memory.
 * </p>
 * <p>
 * The system prompt guides the LLM to act as an order and delivery support agent, and
 * instructs it on when to invoke the available tools.
 * </p>
 */
@Configuration
@ConditionalOnProperty(name = "app.chat.provider", havingValue = "google-genai")
public class GoogleGenAiChatModelConfig {

	/**
	 * System prompt that defines the LLM's role and tool usage instructions.
	 * <p>
	 * This prompt instructs the model to:
	 * <ul>
	 * <li>Act as an order and delivery support agent</li>
	 * <li>Use getOrderList for retrieving member orders</li>
	 * <li>Use checkOrderCancellability to verify if an order can be cancelled</li>
	 * <li>Use trackDelivery to provide shipment status information</li>
	 * </ul>
	 * </p>
	 */
	private static final String SYSTEM_PROMPT = """
			당신은 주문/배송 고객 지원 에이전트입니다.
			- 주문 내역/목록 조회(예: "내 주문 보여줘", "주문 목록"): 현재 사용자 ID가 주어지면 getOrderList 도구에 해당 memberId를 넣어 호출하세요.
			- 주문 취소 가능 여부: checkOrderCancellability 도구에 주문번호(ORD-xxxx)를 넣어 호출하세요. 이전 대화에서 나온 주문번호를 사용할 수 있습니다.
			- 배송 조회: trackDelivery 도구에 운송장번호(TRACK-xxxx)를 넣어 호출하세요.
			""";

	/**
	 * Creates and configures a ChatModel for Google GenAI.
	 * <p>
	 * This bean initializes the Google GenAI client with the provided API key, configures
	 * the model name and temperature, and sets up tool calling capabilities.
	 * </p>
	 * @param apiKey the Google GenAI API key from configuration
	 * @param model the model name (e.g., "gemini-2.5-flash-lite")
	 * @param temperature the temperature setting for LLM response generation
	 * @return a configured {@code ChatModel} instance
	 */
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

	/**
	 * Creates a ChatClient with the configured model, tools, and advisors.
	 * <p>
	 * The ChatClient is initialized with:
	 * </p>
	 * <ul>
	 * <li>The configured system prompt</li>
	 * <li>All available A2aTools for tool calling</li>
	 * <li>A MessageChatMemoryAdvisor for conversation context management</li>
	 * <li>A SimpleLoggerAdvisor for request/response logging</li>
	 * </ul>
	 * @param chatModel the underlying chat model
	 * @param chatMemory the conversation memory store
	 * @param tools the list of available A2aTools implementations
	 * @param <T> the type variable for A2aTools subclasses
	 * @return a configured {@code ChatClient} instance
	 */
	@Bean
	public <T extends A2aTool> ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, List<T> tools) {
		return ChatClient.builder(chatModel)
			.defaultSystem(SYSTEM_PROMPT)
			.defaultTools(tools.toArray())
			.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor())
			.build();
	}

}
