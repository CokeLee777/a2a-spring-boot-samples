package com.github.cokelee777.orderagentserver.client;

import com.github.cokelee777.a2a.common.transport.A2aTransport;
import io.a2a.A2A;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * A2A Protocol client for communicating with the delivery agent.
 *
 * <p>
 * Sends agent-to-agent requests to the delivery agent to query delivery status for
 * shipments. Transport concerns are delegated to {@link A2aTransport}.
 * </p>
 */
@Slf4j
@Component
public class A2aDeliveryAgentClient {

	private static final Pattern STATUS_LINE = Pattern.compile("status:(.+)");

	private final A2aTransport transport;

	private final int timeoutSeconds;

	/**
	 * Constructs a delivery agent client with the delivery agent's base URL and timeout.
	 * @param deliveryAgentBaseUrl the base URL of the delivery agent
	 * @param timeoutSeconds maximum seconds to wait for a response
	 */
	public A2aDeliveryAgentClient(@Value("${order-agent.delivery-agent-url}") String deliveryAgentBaseUrl,
			@Value("${a2a.client.timeout-seconds}") int timeoutSeconds) {
		this.transport = new A2aTransport(deliveryAgentBaseUrl);
		this.timeoutSeconds = timeoutSeconds;
	}

	/**
	 * Retrieves delivery status information for a shipment.
	 *
	 * <p>
	 * Returns {@code null} if the agent call fails or the response cannot be parsed.
	 * </p>
	 * @param trackingNumber the tracking number to query
	 * @return a {@link DeliveryStatusResponse}, or {@code null} on failure
	 */
	public DeliveryStatusResponse getDeliveryStatus(String trackingNumber) {
		return transport.send(A2A.toAgentMessage(trackingNumber), timeoutSeconds)
			.filter(text -> !text.isBlank())
			.map(text -> {
				Matcher m = STATUS_LINE.matcher(text.trim());
				if (m.find()) {
					return new DeliveryStatusResponse(trackingNumber, m.group(1).trim(), null);
				}
				return null;
			})
			.orElseGet(() -> {
				log.error("배송 에이전트 호출 실패 (trackingNumber={})", trackingNumber);
				return null;
			});
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
