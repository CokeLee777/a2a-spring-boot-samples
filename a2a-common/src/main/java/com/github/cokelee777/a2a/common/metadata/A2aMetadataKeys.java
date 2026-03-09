package com.github.cokelee777.a2a.common.metadata;

/**
 * Constants for A2A message metadata keys.
 * <p>
 * Provides shared key names used when building A2A messages with metadata and when
 * reading metadata from incoming messages.
 * </p>
 */
public final class A2aMetadataKeys {

	/**
	 * Metadata key for the skill ID.
	 * <p>
	 * Callers include this key in message metadata to indicate which skill the receiving
	 * agent should invoke. The value must match a skill ID declared in the target agent's
	 * {@code AgentCard}.
	 * </p>
	 */
	public static final String SKILL_ID = "skillId";

	private A2aMetadataKeys() {
	}

}
