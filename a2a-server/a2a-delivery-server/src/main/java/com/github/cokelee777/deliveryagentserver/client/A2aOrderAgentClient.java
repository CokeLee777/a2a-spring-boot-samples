package com.github.cokelee777.deliveryagentserver.client;

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
import com.github.cokelee777.a2a.common.util.TextExtractor;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * A2A client for communicating with the order agent.
 * <p>
 * This client sends internal requests to the order agent to retrieve order information by
 * tracking number, enriching delivery responses with order details.
 * </p>
 */
@Slf4j
@Component
public class A2aOrderAgentClient {

	private final String orderAgentBaseUrl;

	private volatile AgentCard orderAgentCard;

	@Value("${a2a.client.timeout-seconds}")
	private int timeoutSeconds;

	/**
	 * Constructs an A2aOrderAgentClient with the order agent URL.
	 * @param orderAgentBaseUrl the base URL of the order agent
	 */
	public A2aOrderAgentClient(@Value("${delivery-agent.order-agent-url}") String orderAgentBaseUrl) {
		this.orderAgentBaseUrl = orderAgentBaseUrl;
	}

	/**
	 * Resolves and caches the order agent card.
	 * @return the cached or newly resolved {@code AgentCard}
	 */
	private AgentCard resolveAgentCard() {
		if (orderAgentCard == null) {
			synchronized (this) {
				if (orderAgentCard == null) {
					A2AHttpClient httpClient = A2AHttpClientFactory.create();
					orderAgentCard = new A2ACardResolver(httpClient, orderAgentBaseUrl, null).getAgentCard();
					log.info("Order agent card resolved: {}", orderAgentCard.name());
				}
			}
		}
		return orderAgentCard;
	}

	/**
	 * Retrieves order information by tracking number from the order agent.
	 * @param trackingNumber the tracking number to query
	 * @return an OrderInfoResponse containing order details, or null if not found or on
	 * error
	 */
	public OrderInfoResponse getOrderInfo(String trackingNumber) {
		try {
			ClientConfig clientConfig = new ClientConfig.Builder().setAcceptedOutputModes(List.of("text")).build();

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
				.clientConfig(clientConfig)
				.withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
				.addConsumers(consumers)
				.build()) {
				Message message = A2A.toAgentMessage(trackingNumber);
				client.sendMessage(message);
			}

			String responseText = resultFuture.get(timeoutSeconds, TimeUnit.SECONDS);
			if (responseText == null || responseText.isBlank()) {
				return null;
			}
			Map<String, String> parsed = new HashMap<>();
			for (String line : responseText.split("\n")) {
				int colon = line.indexOf(':');
				if (colon > 0) {
					parsed.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
				}
			}
			String orderNumber = parsed.get("orderNumber");
			String orderDate = parsed.get("orderDate");
			String status = parsed.get("status");
			if (orderNumber == null || "NOT_FOUND".equals(orderNumber)) {
				return null;
			}
			return new OrderInfoResponse(orderNumber, parsed.get("productName"), status, orderDate, trackingNumber);
		}
		catch (Exception e) {
			log.error("주문 에이전트 호출 실패 (trackingNumber={}): {}", trackingNumber, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Represents order information retrieved from the order agent.
	 *
	 * @param orderNumber the order number
	 * @param productName the product name
	 * @param status the order status
	 * @param orderDate the order date
	 * @param trackingNumber the tracking number associated with this order
	 */
	public record OrderInfoResponse(String orderNumber, String productName, String status, String orderDate,
			String trackingNumber) {
	}

}
