package com.github.cokelee777.a2aclient;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatOrchestrator {

	private final ChatClient chatClient;

	/**
	 * 세션 기반으로 사용자 메시지를 처리하고 응답과 sessionId를 반환합니다.
	 * @param request 채팅 요청 (message 필수, sessionId 없으면 새로 생성)
	 * @return 응답 텍스트와 사용된 sessionId
	 */
	public ChatResponse handle(ChatRequest request) {
		try {
			String content = chatClient.prompt()
				.advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.sessionId()))
				.user(request.message())
				.call()
				.content();
			return new ChatResponse(content, request.sessionId());
		}
		catch (Exception e) {
			String fallback = "처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
			return new ChatResponse(fallback, request.sessionId());
		}
	}

}
