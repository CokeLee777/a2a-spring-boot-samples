package com.github.cokelee777.deliveryagentserver.client;

import com.github.cokelee777.a2a.common.metadata.A2aMetadataKeys;
import com.github.cokelee777.a2a.common.transport.A2aTransport;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A2A client for communicating with the order agent.
 *
 * <p>
 * Sends internal requests to the order agent to retrieve order information by tracking
 * number. Transport concerns are delegated to {@link A2aTransport}.
 * </p>
 */
@Slf4j
@Component
public class A2aOrderAgentClient {

	private final A2aTransport transport;

	private final int timeoutSeconds;

	/**
	 * Constructs an A2aOrderAgentClient with the order agent URL and timeout.
	 * @param orderAgentBaseUrl the base URL of the order agent
	 * @param timeoutSeconds maximum seconds to wait for a response
	 */
	public A2aOrderAgentClient(@Value("${delivery-agent.order-agent-url}") String orderAgentBaseUrl,
			@Value("${a2a.client.timeout-seconds}") int timeoutSeconds) {
		this.transport = new A2aTransport(orderAgentBaseUrl);
		this.timeoutSeconds = timeoutSeconds;
	}

	/**
	 * Retrieves order information by tracking number from the order agent.
	 *
	 * <p>
	 * Sends an internal A2A request (ROLE_AGENT) with skill ID
	 * {@code "order_info_by_tracking"} to the order agent.
	 * </p>
	 * @param trackingNumber the tracking number to query
	 * @return an {@link OrderInfoResponse} containing order details, or {@code null} if
	 * not found or on error
	 */
	public OrderInfoResponse getOrderInfo(String trackingNumber) {
		Message message = Message.builder()
			.role(Message.Role.ROLE_AGENT)
			.parts(List.of(new TextPart(trackingNumber)))
			.metadata(Map.of(A2aMetadataKeys.SKILL_ID, "order_info_by_tracking"))
			.build();
		return transport.send(message, timeoutSeconds)
			.filter(text -> !text.isBlank())
			.map(text -> parseOrderInfo(text, trackingNumber))
			.orElseGet(() -> {
				log.error("주문 에이전트 호출 실패 (trackingNumber={})", trackingNumber);
				return null;
			});
	}

	private OrderInfoResponse parseOrderInfo(String responseText, String trackingNumber) {
		Map<String, String> parsed = new HashMap<>();
		for (String line : responseText.split("\n")) {
			int colon = line.indexOf(':');
			if (colon > 0) {
				parsed.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
			}
		}
		String orderNumber = parsed.get("orderNumber");
		if (orderNumber == null || "NOT_FOUND".equals(orderNumber)) {
			return null;
		}
		return new OrderInfoResponse(orderNumber, parsed.get("productName"), parsed.get("status"),
				parsed.get("orderDate"), trackingNumber);
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
