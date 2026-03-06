package com.github.cokelee777.a2aclient;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for handling chat requests.
 * <p>
 * This controller exposes a single endpoint for chat interactions. It delegates the
 * actual chat processing to {@link ChatOrchestrator} and provides error handling with
 * appropriate HTTP status codes.
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class ChatController {

	private final ChatOrchestrator chatOrchestrator;

	/**
	 * Handles a chat request and returns a response.
	 * <p>
	 * This endpoint accepts a chat request with a message and optional session ID. It
	 * maintains conversation context across multiple requests using the session ID. If an
	 * unexpected error occurs, it returns a 500 status with an error message.
	 * </p>
	 * @param request the chat request containing message and optional session ID
	 * @return a {@code ResponseEntity} containing the chat response with the same session
	 * ID
	 */
	@PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<ChatResponse> chat(@Valid @RequestBody ChatRequest request) {
		try {
			ChatResponse response = chatOrchestrator.handle(request);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
			return ResponseEntity.status(500)
				.body(new ChatResponse(request.sessionId(), String.format("오류가 발생했습니다: %s", errorMessage)));
		}
	}

}
