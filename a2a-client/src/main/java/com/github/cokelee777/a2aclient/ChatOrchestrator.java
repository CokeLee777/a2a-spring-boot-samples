package com.github.cokelee777.a2aclient;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

/**
 * Service for orchestrating chat interactions with the LLM.
 * <p>
 * This service handles the core logic of processing user messages and generating
 * responses. It integrates with the Spring AI {@code ChatClient} to leverage LLM
 * capabilities and manages conversation context using session-based memory.
 * </p>
 */
@Service
@RequiredArgsConstructor
public class ChatOrchestrator {

	private final ChatClient chatClient;

	/**
	 * Processes a chat request and returns a response from the LLM.
	 * <p>
	 * This method:
	 * </p>
	 * <ol>
	 * <li>Prepends the member ID context to the user message if available</li>
	 * <li>Calls the Spring AI ChatClient to generate a response</li>
	 * <li>Associates the conversation with a session ID for memory management</li>
	 * <li>Returns the generated response or a fallback error message on failure</li>
	 * </ol>
	 * @param request the chat request containing the message and session ID
	 * @return a {@code ChatResponse} containing the LLM-generated response and session ID
	 */
	public ChatResponse handle(ChatRequest request) {
		try {
			String userMessage = request.message();
			if (request.memberId() != null && !request.memberId().isBlank()) {
				userMessage = "[현재 사용자 ID: " + request.memberId() + "]\n\n" + userMessage;
			}
			String content = chatClient.prompt()
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.sessionId()))
				.user(userMessage)
				.call()
				.content();
			return new ChatResponse(request.sessionId(), content);
		}
		catch (Exception e) {
			return new ChatResponse(request.sessionId(), e.getMessage());
		}
	}

}
