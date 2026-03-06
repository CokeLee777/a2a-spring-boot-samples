package com.github.cokelee777.paymentagentserver.executor;

/**
 * Interface for handling skill execution requests on the payment agent.
 * <p>
 * Implementations determine whether they can handle a given message and execute the
 * corresponding skill logic.
 * </p>
 */
public interface SkillExecutor {

	/**
	 * Determines whether this executor can handle the given message.
	 * @param message the user message text
	 * @param isInternalCall whether this is an internal agent-to-agent call
	 * @return true if this executor can handle the message, false otherwise
	 */
	boolean canHandle(String message, boolean isInternalCall);

	/**
	 * Executes the skill logic for the given message.
	 * @param message the user message text
	 * @param isInternalCall whether this is an internal agent-to-agent call
	 * @return the result of the skill execution
	 */
	String execute(String message, boolean isInternalCall);

}
