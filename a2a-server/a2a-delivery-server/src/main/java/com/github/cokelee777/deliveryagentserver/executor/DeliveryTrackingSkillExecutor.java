package com.github.cokelee777.deliveryagentserver.executor;

import com.github.cokelee777.deliveryagentserver.client.A2aOrderAgentClient;
import com.github.cokelee777.deliveryagentserver.db.DeliveryDatabase;
import io.a2a.spec.Message;
import org.springframework.stereotype.Component;

/**
 * Skill executor for delivery tracking queries.
 * <p>
 * Handles the {@code track_delivery} skill. Only accessible to external user calls
 * (ROLE_USER). Enriches the delivery response with order information fetched from the
 * order agent.
 * </p>
 */
@Component
public class DeliveryTrackingSkillExecutor implements SkillExecutor {

	private final A2aOrderAgentClient orderAgentClient;

	/**
	 * Constructs a DeliveryTrackingSkillExecutor with an order agent client.
	 * @param orderAgentClient the client for communicating with the order agent
	 */
	public DeliveryTrackingSkillExecutor(A2aOrderAgentClient orderAgentClient) {
		this.orderAgentClient = orderAgentClient;
	}

	/**
	 * Returns the skill ID handled by this executor.
	 * @return {@code "track_delivery"}
	 */
	@Override
	public String skillId() {
		return "track_delivery";
	}

	/**
	 * Returns the required caller role for this skill.
	 * @return {@link Message.Role#ROLE_USER} — external calls only
	 */
	@Override
	public Message.Role requiredRole() {
		return Message.Role.ROLE_USER;
	}

	/**
	 * Executes the delivery tracking lookup, enriched with order information.
	 * <p>
	 * Fetches the delivery status and, if available, appends order details from the order
	 * agent for a user-friendly response.
	 * </p>
	 * @param message the message text containing a tracking number (TRACK-xxx)
	 * @return a formatted delivery status response, optionally enriched with order info
	 */
	@Override
	public String execute(String message) {
		String trackingNumber = extractTrackingNumber(message);
		String baseResult = DeliveryDatabase.lookup(trackingNumber);

		var orderInfo = orderAgentClient.getOrderInfo(trackingNumber);
		if (orderInfo != null) {
			return baseResult + String.format("\n\n[주문 에이전트 연동 정보]\n주문번호: %s\n주문일시: %s\n주문상태: %s",
					orderInfo.orderNumber(), orderInfo.orderDate(), orderInfo.status());
		}

		return baseResult;
	}

	/**
	 * Extracts the tracking number from the message text.
	 * @param text the message text
	 * @return the first word starting with "TRACK-", or the trimmed text
	 */
	private String extractTrackingNumber(String text) {
		for (String word : text.split("\\s+")) {
			if (word.startsWith("TRACK-")) {
				return word;
			}
		}
		return text.trim();
	}

}
