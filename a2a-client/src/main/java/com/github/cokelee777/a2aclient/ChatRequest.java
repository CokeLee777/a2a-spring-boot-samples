package com.github.cokelee777.a2aclient;

import jakarta.validation.constraints.NotBlank;

import java.util.Objects;
import java.util.UUID;

/**
 * Represents a chat request from the client.
 * <p>
 * This record encapsulates the session ID, member ID, and message content. If no session
 * ID is provided, a new UUID is automatically generated to start a new conversation.
 * </p>
 *
 * @param sessionId the unique identifier for the conversation session; if null, a new
 * UUID is generated
 * @param memberId the ID of the member sending the message; must not be blank
 * @param message the message content from the user; must not be blank
 */
public record ChatRequest(String sessionId, @NotBlank(message = "memberId must not be blank") String memberId,
		@NotBlank(message = "message must not be blank") String message) {

	/**
	 * Compact constructor that initializes {@code sessionId} with a new UUID if not
	 * provided.
	 * @param sessionId the session ID; a new UUID is generated if null
	 * @param memberId the member ID of the user sending the message
	 * @param message the message content
	 */
	public ChatRequest {
		sessionId = Objects.requireNonNullElse(sessionId, UUID.randomUUID().toString());
	}
}
