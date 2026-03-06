package com.github.cokelee777.a2aclient;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class ChatController {

	private final ChatOrchestratorService chatOrchestratorService;

	/**
	 * 자유 문의를 받아 LLM으로 의도를 분석한 뒤, 해당 A2A 에이전트를 호출해 결과를 반환합니다.
	 * <p>
	 * 요청 예: POST /api/chat Body: {"message": "ORD-1001 주문 취소해줘"} 응답: {"response": "에이전트가
	 * 반환한 텍스트"}
	 */
	@PostMapping(value = "/api/chat", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> chat(@RequestBody String message) {
		try {
			String response = chatOrchestratorService.handleUserQuery(message);
			return ResponseEntity.ok(response);
		}
		catch (Exception e) {
			String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
			return ResponseEntity.status(500).body(String.format("오류가 발생했습니다: %s", errorMessage));
		}
	}

}
