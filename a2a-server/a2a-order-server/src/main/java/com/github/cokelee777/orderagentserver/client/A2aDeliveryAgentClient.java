package com.github.cokelee777.orderagentserver.client;

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
import io.a2a.spec.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A2A Protocol client for communicating with the delivery agent.
 * <p>
 * This component sends agent-to-agent requests to the delivery agent to query delivery
 * status for shipments. It uses the A2A Java SDK to manage the communication protocol and
 * handles responses from the delivery agent.
 * </p>
 */
@Slf4j
@Component
public class A2aDeliveryAgentClient {

	private static final Pattern STATUS_LINE = Pattern.compile("status:(.+)");

	private final String deliveryAgentBaseUrl;

	private volatile AgentCard deliveryAgentCard;

	@Value("${a2a.client.timeout-seconds}")
	private int timeoutSeconds;

	/**
	 * Constructs a delivery agent client with the delivery agent's base URL.
	 * @param deliveryAgentBaseUrl the base URL of the delivery agent
	 */
	public A2aDeliveryAgentClient(@Value("${order-agent.delivery-agent-url}") String deliveryAgentBaseUrl) {
		this.deliveryAgentBaseUrl = deliveryAgentBaseUrl;
	}

	/**
	 * Resolves and caches the agent card for the delivery agent.
	 * <p>
	 * The agent card is resolved once and cached for subsequent requests. This method
	 * uses double-checked locking to ensure thread-safe initialization.
	 * </p>
	 * @return the resolved agent card for the delivery agent
	 */
	private AgentCard resolveAgentCard() {
		if (deliveryAgentCard == null) {
			synchronized (this) {
				if (deliveryAgentCard == null) {
					A2AHttpClient httpClient = A2AHttpClientFactory.create();
					deliveryAgentCard = new A2ACardResolver(httpClient, deliveryAgentBaseUrl, null).getAgentCard();
					log.info("Delivery agent card resolved: {}", deliveryAgentCard.name());
				}
			}
		}
		return deliveryAgentCard;
	}

	/**
	 * Retrieves delivery status information for a shipment.
	 * <p>
	 * Sends an agent-to-agent request to the delivery agent with the tracking number and
	 * returns the delivery status. If the request fails or times out, returns null.
	 * </p>
	 * @param trackingNumber the tracking number to query
	 * @return a DeliveryStatusResponse containing the tracking number and delivery
	 * status, or null if the request fails
	 */
	public DeliveryStatusResponse getDeliveryStatus(String trackingNumber) {
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
			Matcher m = STATUS_LINE.matcher(responseText.trim());
			if (m.find()) {
				return new DeliveryStatusResponse(trackingNumber, m.group(1).trim(), null);
			}
			return null;
		}
		catch (Exception e) {
			log.error("배송 에이전트 호출 실패 (trackingNumber={}): {}", trackingNumber, e.getMessage(), e);
			return null;
		}
	}

	/**
	 * Response record containing delivery status information for a shipment.
	 *
	 * @param trackingNumber the tracking number that was queried
	 * @param status the current delivery status (e.g., "배송중", "배송완료", "상품준비중")
	 * @param detail additional detail information about the delivery (may be null)
	 */
	public record DeliveryStatusResponse(String trackingNumber, String status, String detail) {
	}

}
