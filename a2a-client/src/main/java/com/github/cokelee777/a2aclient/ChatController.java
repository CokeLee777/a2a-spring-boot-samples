package com.github.cokelee777.a2aclient;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

	private final ChatOrchestrator chatOrchestrator;

	/**
	 * 세션 기반 채팅. 같은 sessionId로 요청하면 대화 이력이 유지됩니다.
	 * <p>
	 * 요청 Body: {@code message}(필수), {@code sessionId}(선택, 없으면 새 세션 생성). 응답 Body:
	 * {@code response}(에이전트 응답 텍스트), {@code sessionId}(이번 요청에 사용된 세션 ID).
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
