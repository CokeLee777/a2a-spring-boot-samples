package com.github.cokelee777.a2aclient.config;

import com.github.cokelee777.a2aclient.tools.A2aTools;
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

@Configuration
@ConditionalOnProperty(name = "app.chat.provider", havingValue = "google-genai")
public class GoogleGenAiChatModelConfig {

	private static final String SYSTEM_PROMPT = """
			당신은 주문/배송 고객 지원 에이전트입니다.
			- 주문 내역/목록 조회(예: "내 주문 보여줘", "주문 목록"): 현재 사용자 ID가 주어지면 getOrderList 도구에 해당 memberId를 넣어 호출하세요.
			- 주문 취소 가능 여부: checkOrderCancellability 도구에 주문번호(ORD-xxxx)를 넣어 호출하세요. 이전 대화에서 나온 주문번호를 사용할 수 있습니다.
			- 배송 조회: trackDelivery 도구에 운송장번호(TRACK-xxxx)를 넣어 호출하세요.
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
	public <T extends A2aTools> ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, List<T> tools) {
		return ChatClient.builder(chatModel)
			.defaultSystem(SYSTEM_PROMPT)
			.defaultTools(tools.toArray())
			.defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build(), new SimpleLoggerAdvisor())
			.build();
	}

}
