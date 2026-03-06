package com.github.cokelee777.a2aclient.tools;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
public abstract class A2aTools {

	protected static final ClientConfig CLIENT_CONFIG = new ClientConfig.Builder()
		.setAcceptedOutputModes(List.of("text"))
		.build();

	private final String agentUrl;

	private final AtomicReference<AgentCard> agentCardRef = new AtomicReference<>();

	@Value("${a2a.client.timeout-seconds}")
	private int timeoutSeconds;

	protected A2aTools(String agentUrl) {
		this.agentUrl = agentUrl;
	}

	protected String sendRequest(String text) {
		try {
			CompletableFuture<String> resultFuture = new CompletableFuture<>();
			List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of((event, card) -> {
				if (event instanceof TaskEvent taskEvent) {
					Task task = taskEvent.getTask();
					if (TaskState.TASK_STATE_FAILED.equals(task.status().state())) {
						resultFuture.complete("처리 중 오류가 발생했습니다.");
						return;
					}
					StringBuilder sb = new StringBuilder();
					if (task.artifacts() != null) {
						task.artifacts().forEach(artifact -> artifact.parts().forEach(part -> {
							if (part instanceof TextPart textPart) {
								sb.append(textPart.text());
							}
						}));
					}
					resultFuture.complete(sb.toString());
				}
			});
			Consumer<Throwable> errorHandler = resultFuture::completeExceptionally;
			try (Client client = Client.builder(resolveAgentCard())
				.clientConfig(CLIENT_CONFIG)
				.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
				.addConsumers(consumers)
				.streamingErrorHandler(errorHandler)
				.build()) {
				client.sendMessage(A2A.toUserMessage(text));
			}
			return resultFuture.get(timeoutSeconds, TimeUnit.SECONDS);
		}
		catch (Exception e) {
			return "에이전트 호출 중 오류: " + e.getMessage();
		}
	}

	private AgentCard resolveAgentCard() {
		AgentCard card = agentCardRef.get();
		if (card == null) {
			synchronized (this) {
				card = agentCardRef.get();
				if (card == null) {
					A2AHttpClient httpClient = A2AHttpClientFactory.create();
					card = new A2ACardResolver(httpClient, agentUrl, null).getAgentCard();
					log.info("{} agent card resolved", card.name());
					agentCardRef.set(card);
				}
			}
		}
		return card;
	}

}
