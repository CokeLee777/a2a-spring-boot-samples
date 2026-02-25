package com.github.cokelee777.a2aclient;

import com.github.cokelee777.a2aclient.orchestrator.ChatOrchestratorService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class ChatController {

    private final ChatOrchestratorService chatOrchestratorService;

    public ChatController(ChatOrchestratorService chatOrchestratorService) {
        this.chatOrchestratorService = chatOrchestratorService;
    }

    /**
     * 자유 문의를 받아 LLM으로 의도를 분석한 뒤, 해당 A2A 에이전트를 호출해 결과를 반환합니다.
     *
     * 요청 예: POST /api/chat  Body: {"message": "ORD-1001 주문 취소해줘"}
     * 응답: {"response": "에이전트가 반환한 텍스트"}
     */
    @PostMapping(value = "/api/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Map<String, String>> chat(@RequestBody(required = false) Map<String, String> body) {
        try {
            String message = body != null ? body.get("message") : null;
            String response = chatOrchestratorService.handleUserQuery(message);
            return ResponseEntity.ok(Map.of("response", response != null ? response : ""));
        } catch (Exception e) {
            String errorMessage = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            return ResponseEntity.status(500)
                    .body(Map.of("response", "오류가 발생했습니다: " + errorMessage));
        }
    }
}
