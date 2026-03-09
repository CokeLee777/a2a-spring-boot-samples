package com.github.cokelee777.a2a.common.transport;

import com.github.cokelee777.a2a.common.util.TextExtractor;
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
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

/**
 * Reusable A2A transport that handles AgentCard resolution, caching, and the
 * CompletableFuture-based send-and-await pattern.
 *
 * <p>
 * Instances are per-target-agent: each instance holds the URL and its own cached
 * AgentCard. Thread-safe lazy initialization is done via double-checked locking with
 * {@link AtomicReference}.
 * </p>
 */
@Slf4j
public class A2aTransport {

	private static final ClientConfig CLIENT_CONFIG = new ClientConfig.Builder().setAcceptedOutputModes(List.of("text"))
		.build();

	private final String agentUrl;

	private final AtomicReference<AgentCard> agentCardRef = new AtomicReference<>();

	/**
	 * Creates a transport targeting the given agent URL.
	 * @param agentUrl the base URL of the target agent
	 */
	public A2aTransport(String agentUrl) {
		this.agentUrl = agentUrl;
	}

	/**
	 * Sends a message to the target agent and waits for the result.
	 *
	 * <p>
	 * Returns {@link Optional#empty()} when the task fails or an exception occurs, so
	 * callers can provide their own domain-specific fallback.
	 * </p>
	 * @param message the A2A message to send (user or agent role)
	 * @param timeoutSeconds maximum seconds to wait for the response
	 * @return the response text, or empty if the agent reported failure or an error
	 * occurred
	 */
	public Optional<String> send(Message message, int timeoutSeconds) {
		try {
			CompletableFuture<String> resultFuture = new CompletableFuture<>();
			List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of((event, card) -> {
				if (event instanceof TaskEvent taskEvent) {
					Task task = taskEvent.getTask();
					if (TaskState.TASK_STATE_FAILED.equals(task.status().state())) {
						resultFuture.complete(null);
						return;
					}
					resultFuture.complete(TextExtractor.extractFromTask(task));
				}
			});
			try (Client client = Client.builder(resolveAgentCard())
				.clientConfig(CLIENT_CONFIG)
				.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
				.addConsumers(consumers)
				.streamingErrorHandler(resultFuture::completeExceptionally)
				.build()) {
				client.sendMessage(message);
			}
			return Optional.ofNullable(resultFuture.get(timeoutSeconds, TimeUnit.SECONDS));
		}
		catch (Exception e) {
			log.warn("A2A transport error (url={}): {}", agentUrl, e.getMessage());
			return Optional.empty();
		}
	}

	/**
	 * Resolves and caches the AgentCard for this transport's target agent.
	 *
	 * <p>
	 * Uses double-checked locking with {@link AtomicReference} for thread-safe lazy
	 * initialization.
	 * </p>
	 * @return the resolved {@link AgentCard}
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
