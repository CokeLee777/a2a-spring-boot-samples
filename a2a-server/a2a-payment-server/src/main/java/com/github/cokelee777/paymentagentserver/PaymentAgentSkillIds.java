package com.github.cokelee777.paymentagentserver;

/**
 * Skill ID constants for the payment agent.
 * <p>
 * These values must match the skill IDs declared in the payment agent's
 * {@code AgentCard}. Use these constants within this module only; callers from other
 * modules reference the skill ID as a plain string (protocol-level contract).
 * </p>
 */
public final class PaymentAgentSkillIds {

	/**
	 * Skill ID for retrieving payment and refund eligibility status (internal A2A only).
	 */
	public static final String PAYMENT_STATUS = "payment_status";

	private PaymentAgentSkillIds() {
	}

}
