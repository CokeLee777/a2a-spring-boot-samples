package com.github.cokelee777.deliveryagentserver;

/**
 * Skill ID constants for the delivery agent.
 * <p>
 * These values must match the skill IDs declared in the delivery agent's
 * {@code AgentCard}. Use these constants within this module only; callers from other
 * modules reference the skill ID as a plain string (protocol-level contract).
 * </p>
 */
public final class DeliveryAgentSkillIds {

	/** Skill ID for tracking a delivery by tracking number (external calls). */
	public static final String TRACK_DELIVERY = "track_delivery";

	/** Skill ID for querying delivery status (internal A2A only). */
	public static final String DELIVERY_STATUS_INTERNAL = "delivery_status_internal";

	private DeliveryAgentSkillIds() {
	}

}
