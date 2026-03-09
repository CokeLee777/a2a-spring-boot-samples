package com.github.cokelee777.paymentagentserver.executor;

import com.github.cokelee777.paymentagentserver.db.PaymentDatabase;
import io.a2a.spec.Message;
import org.springframework.stereotype.Component;

/**
 * Skill executor for retrieving payment and refund eligibility information.
 * <p>
 * Handles the {@code payment_status} skill. Only accessible to internal A2A calls
 * (ROLE_AGENT) from the order agent.
 * </p>
 */
@Component
public class PaymentStatusSkillExecutor implements SkillExecutor {

	/**
	 * Returns the skill ID handled by this executor.
	 * @return {@code "payment_status"}
	 */
	@Override
	public String skillId() {
		return "payment_status";
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
	 * Executes the payment information lookup by order number.
	 * <p>
	 * Queries the payment database and returns refund eligibility in a key-value format
	 * for internal A2A consumption.
	 * </p>
	 * @param message the order number (ORD-xxx)
	 * @return a formatted string with refund eligibility status
	 */
	@Override
	public String execute(String message) {
		return PaymentDatabase.getRefundEligibleLine(message.trim());
	}

}
