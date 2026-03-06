package com.github.cokelee777.orderagentserver.executor;

import com.github.cokelee777.orderagentserver.db.OrderDatabase;
import org.springframework.stereotype.Component;

/**
 * Skill executor for retrieving order information by tracking number.
 * <p>
 * This executor handles internal agent-to-agent requests to look up order details using a
 * tracking number. It is only accessible to internal A2A calls, not external user
 * requests.
 * </p>
 */
@Component
public class OrderInfoInternalExecutor implements SkillExecutor {

	/**
	 * Determines whether this executor can handle internal order info requests.
	 * <p>
	 * This executor handles internal requests starting with "TRACK-" prefix.
	 * </p>
	 * @param message the user message text
	 * @param isInternalCall whether this is an internal agent-to-agent call
	 * @return true if the message starts with "TRACK-" and is an internal call
	 */
	@Override
	public boolean canHandle(String message, boolean isInternalCall) {
		return isInternalCall && message != null && message.startsWith("TRACK-");
	}

	/**
	 * Executes the order information lookup by tracking number.
	 * <p>
	 * Queries the order database for an order with the given tracking number and returns
	 * order details in a key-value format for internal A2A consumption.
	 * </p>
	 * @param message the tracking number (TRACK-xxx)
	 * @param isInternalCall whether this is an internal agent-to-agent call
	 * @return a formatted string with order details, or a NOT_FOUND response if not found
	 */
	@Override
	public String execute(String message, boolean isInternalCall) {
		String trackingNumber = message.trim();
		return OrderDatabase.findByTrackingNumber(trackingNumber)
			.map(order -> "orderNumber:" + order.orderNumber() + "\n" + "productName:" + order.productName() + "\n"
					+ "orderDate:" + order.orderDate() + "\n" + "status:" + order.status())
			.orElse("orderNumber:NOT_FOUND\norderDate:\nstatus:NOT_FOUND");
	}

}
