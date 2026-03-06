package com.github.cokelee777.a2aclient.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring AI tools for delivery-related operations.
 * <p>
 * This component extends {@link A2aTool} to provide LLM-callable tools for delivery
 * tracking. This tool communicates with the delivery agent via the A2A Protocol to
 * retrieve shipping and delivery status information.
 * </p>
 */
@Component
public class DeliveryTool extends A2aTool {

	/**
	 * Request object for tracking a delivery.
	 *
	 * @param trackingNumber the tracking number in format "TRACK-xxxx"
	 */
	public record DeliveryRequest(String trackingNumber) {
	}

	/**
	 * Constructs a DeliveryTools instance with the configured delivery agent URL.
	 * @param agentUrl the base URL of the delivery agent
	 */
	public DeliveryTool(@Value("${a2a.delivery-agent-url}") String agentUrl) {
		super(agentUrl);
	}

	/**
	 * Tracks the delivery status of a shipment.
	 * <p>
	 * This tool queries the delivery agent to retrieve the current status and location
	 * information for a shipment identified by its tracking number. It is typically
	 * invoked when the user inquires about delivery status.
	 * </p>
	 * @param request the request containing the tracking number (e.g., "TRACK-1001")
	 * @return a formatted response from the delivery agent with shipment status details
	 */
	@Tool(description = "배송 조회. 운송장번호(TRACK-xxxx)가 필요합니다.")
	public String trackDelivery(DeliveryRequest request) {
		return sendRequest(request.trackingNumber() + " 배송 조회");
	}

}
