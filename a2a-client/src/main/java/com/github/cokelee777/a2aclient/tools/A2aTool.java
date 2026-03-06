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

/**
 * Abstract base class for A2A (Agent-to-Agent) protocol tools.
 * <p>
 * This class provides common functionality for communicating with remote agents via the
 * A2A Protocol over JSON-RPC. Subclasses implement specific agent operations by extending
 * this base and using the {@link #sendRequest(String)} method.
 * </p>
 * <p>
 * Features:
 * </p>
 * <ul>
 * <li>Lazy initialization and caching of agent cards</li>
 * <li>Configurable request timeout via {@code a2a.client.timeout-seconds}</li>
 * <li>Error handling with fallback messages</li>
 * <li>Support for streaming responses with task event processing</li>
 * </ul>
 */
@Slf4j
public abstract class A2aTool {

	/**
	 * Default client configuration that accepts text-based output modes.
	 */
	protected static final ClientConfig CLIENT_CONFIG = new ClientConfig.Builder()
		.setAcceptedOutputModes(List.of("text"))
		.build();

	private final String agentUrl;

	private final AtomicReference<AgentCard> agentCardRef = new AtomicReference<>();

	@Value("${a2a.client.timeout-seconds}")
	private int timeoutSeconds;

	/**
	 * Constructs an A2aTools instance with the target agent URL.
	 * @param agentUrl the base URL of the target agent
	 */
	protected A2aTool(String agentUrl) {
		this.agentUrl = agentUrl;
	}

	/**
	 * Sends a request to the remote agent and retrieves the response.
	 * <p>
	 * This method:
	 * </p>
	 * <ol>
	 * <li>Establishes a connection to the agent using the A2A Protocol</li>
	 * <li>Sends the user message to the agent</li>
	 * <li>Waits for the task completion with a configurable timeout</li>
	 * <li>Extracts and returns the text response from task artifacts</li>
	 * <li>Returns an error message if the agent fails or the request times out</li>
	 * </ol>
	 * @param text the request message to send to the agent
	 * @return the response text from the agent, or an error message if the request fails
	 */
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

	/**
	 * Resolves and caches the agent card for the target agent.
	 * <p>
	 * The agent card is fetched from the remote agent's well-known endpoint and cached in
	 * memory to avoid repeated HTTP requests. Initialization is synchronized to ensure
	 * thread-safe lazy loading.
	 * </p>
	 * @return the cached or newly resolved {@code AgentCard} for this agent
	 */
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
