package com.github.cokelee777.orderagentserver.executor;

import com.github.cokelee777.a2a.server.common.executor.SkillExecutor;
import com.github.cokelee777.orderagentserver.OrderAgentSkillIds;
import com.github.cokelee777.orderagentserver.db.OrderDatabase;
import io.a2a.spec.Message;
import org.springframework.stereotype.Component;

/**
 * Skill executor for retrieving order information by tracking number.
 * <p>
 * Handles the {@code order_info_by_tracking} skill. Only accessible to internal A2A calls
 * (ROLE_AGENT) from the delivery agent.
 * </p>
 */
@Component
public class OrderInfoSkillExecutor implements SkillExecutor {

	/**
	 * Returns the skill ID handled by this executor.
	 * @return {@link OrderAgentSkillIds#ORDER_INFO_BY_TRACKING}
	 */
	@Override
	public String skillId() {
		return OrderAgentSkillIds.ORDER_INFO_BY_TRACKING;
	}

	/**
	 * Returns the required caller role for this skill.
	 * @return {@link Message.Role#ROLE_AGENT} — internal calls only
	 */
	@Override
	public Message.Role requiredRole() {
		return Message.Role.ROLE_AGENT;
	}

	/**
	 * Executes the order information lookup by tracking number.
	 * <p>
	 * Returns order details in a key-value format for internal A2A consumption.
	 * </p>
	 * @param message the tracking number (TRACK-xxx)
	 * @return a key-value formatted string with order details, or NOT_FOUND values
	 */
	@Override
	public String execute(String message) {
		String trackingNumber = message.trim();
		return OrderDatabase.findByTrackingNumber(trackingNumber)
			.map(order -> "orderNumber:" + order.orderNumber() + "\n" + "productName:" + order.productName() + "\n"
					+ "orderDate:" + order.orderDate() + "\n" + "status:" + order.status())
			.orElse("orderNumber:NOT_FOUND\norderDate:\nstatus:NOT_FOUND");
	}

}
