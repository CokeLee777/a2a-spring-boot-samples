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
import java.util.regex.Pattern;

/**
 * A2A Protocol client for communicating with the payment agent.
 * <p>
 * This component sends agent-to-agent requests to the payment agent to query payment and
 * refund eligibility for orders. It uses the A2A Java SDK to manage the communication
 * protocol and handles responses from the payment agent.
 * </p>
 */
@Slf4j
@Component
public class A2aPaymentAgentClient {

	private static final Pattern REFUND_ELIGIBLE_LINE = Pattern.compile("refundEligible:(true|false)");

	private final String paymentAgentBaseUrl;

	private volatile AgentCard paymentAgentCard;

	@Value("${a2a.client.timeout-seconds}")
	private int timeoutSeconds;

	/**
	 * Constructs a payment agent client with the payment agent's base URL.
	 * @param paymentAgentBaseUrl the base URL of the payment agent
	 */
	public A2aPaymentAgentClient(@Value("${order-agent.payment-agent-url}") String paymentAgentBaseUrl) {
		this.paymentAgentBaseUrl = paymentAgentBaseUrl;
	}

	/**
	 * Resolves and caches the agent card for the payment agent.
	 * <p>
	 * The agent card is resolved once and cached for subsequent requests. This method
	 * uses double-checked locking to ensure thread-safe initialization.
	 * </p>
	 * @return the resolved agent card for the payment agent
	 */
	private AgentCard resolveAgentCard() {
		if (paymentAgentCard == null) {
			synchronized (this) {
				if (paymentAgentCard == null) {
					A2AHttpClient httpClient = A2AHttpClientFactory.create();
					paymentAgentCard = new A2ACardResolver(httpClient, paymentAgentBaseUrl, null).getAgentCard();
					log.info("Payment agent card resolved: {}", paymentAgentCard.name());
				}
			}
		}
		return paymentAgentCard;
	}

	/**
	 * Retrieves payment and refund eligibility status for an order.
	 * <p>
	 * Sends an agent-to-agent request to the payment agent with the order number and
	 * returns the refund eligibility status. If the request fails or times out, returns a
	 * response indicating the order is not refund-eligible.
	 * </p>
	 * @param orderNumber the order number to query
	 * @return a PaymentStatusResponse containing the order number and refund eligibility
	 * status
	 */
	public PaymentStatusResponse getPaymentStatus(String orderNumber) {
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
				Message message = A2A.toAgentMessage(orderNumber);
				client.sendMessage(message);
			}

			String responseText = resultFuture.get(timeoutSeconds, TimeUnit.SECONDS);
			if (responseText == null || responseText.isBlank()) {
				return new PaymentStatusResponse(orderNumber, false);
			}
			var m = REFUND_ELIGIBLE_LINE.matcher(responseText.trim());
			if (m.find()) {
				return new PaymentStatusResponse(orderNumber, Boolean.parseBoolean(m.group(1)));
			}
			return new PaymentStatusResponse(orderNumber, false);
		}
		catch (Exception e) {
			log.error("결제 에이전트 호출 실패 (orderNumber={}): {}", orderNumber, e.getMessage(), e);
			return new PaymentStatusResponse(orderNumber, false);
		}
	}

	/**
	 * Response record containing payment status information for an order.
	 *
	 * @param orderNumber the order number that was queried
	 * @param refundEligible whether the order is eligible for refund
	 */
	public record PaymentStatusResponse(String orderNumber, boolean refundEligible) {
	}

}
