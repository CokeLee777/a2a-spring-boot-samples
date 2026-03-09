package com.github.cokelee777.orderagentserver;

/**
 * Skill ID constants for the order agent.
 * <p>
 * These values must match the skill IDs declared in the order agent's {@code AgentCard}.
 * Use these constants within this module only; callers from other modules reference the
 * skill ID as a plain string (protocol-level contract).
 * </p>
 */
public final class OrderAgentSkillIds {

	/** Skill ID for listing orders by member ID. */
	public static final String ORDER_LIST = "order_list";

	/** Skill ID for checking order cancellation eligibility. */
	public static final String ORDER_CANCELLABILITY_CHECK = "order_cancellability_check";

	/** Skill ID for retrieving order info by tracking number (internal A2A only). */
	public static final String ORDER_INFO_BY_TRACKING = "order_info_by_tracking";

	private OrderAgentSkillIds() {
	}

}
