package com.github.cokelee777.deliveryagentserver.executor;

import com.github.cokelee777.deliveryagentserver.db.DeliveryDatabase;
import io.a2a.spec.Message;
import org.springframework.stereotype.Component;

/**
 * Skill executor for delivery status queries.
 * <p>
 * Handles the {@code delivery_status_internal} skill. Only accessible to internal A2A
 * calls (ROLE_AGENT) from the order agent. Returns a minimal key-value formatted status
 * string for machine consumption.
 * </p>
 */
@Component
public class DeliveryStatusSkillExecutor implements SkillExecutor {

	/**
	 * Returns the skill ID handled by this executor.
	 * @return {@code "delivery_status_internal"}
	 */
	@Override
	public String skillId() {
		return "delivery_status_internal";
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
	 * Executes the delivery status lookup by tracking number.
	 * <p>
	 * Returns a key-value formatted string for internal A2A consumption.
	 * </p>
	 * @param message the tracking number (TRACK-xxx)
	 * @return a key-value string such as {@code status:배송중}, or {@code status:NOT_FOUND}
	 */
	@Override
	public String execute(String message) {
		return DeliveryDatabase.findById(message.trim())
			.map(info -> "status:" + info.status())
			.orElse("status:NOT_FOUND");
	}

}
