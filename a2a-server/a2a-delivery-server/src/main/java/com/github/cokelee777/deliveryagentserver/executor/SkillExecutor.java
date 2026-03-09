package com.github.cokelee777.deliveryagentserver.executor;

import io.a2a.spec.Message;

/**
 * Interface for handling skill execution requests on the delivery agent.
 * <p>
 * Each implementation declares the skill ID it handles and the expected caller role,
 * allowing the agent executor to route requests by skill ID without inspecting message
 * content.
 * </p>
 */
public interface SkillExecutor {

	/**
	 * Returns the skill ID this executor handles.
	 * <p>
	 * Must match the {@code id} declared in the agent's {@code AgentCard} skills list.
	 * </p>
	 * @return the skill ID string (e.g., {@code "track_delivery"})
	 */
	String skillId();

	/**
	 * Returns the A2A caller role required to invoke this skill.
	 * <p>
	 * Used by the agent executor to enforce access control: requests with a different
	 * role are rejected without invoking {@link #execute(String)}.
	 * </p>
	 * @return {@link Message.Role#ROLE_AGENT} for internal skills,
	 * {@link Message.Role#ROLE_USER} for external skills
	 */
	Message.Role requiredRole();

	/**
	 * Executes the skill logic for the given message text.
	 * @param message the message text extracted from the A2A request
	 * @return the result of the skill execution
	 */
	String execute(String message);

}
