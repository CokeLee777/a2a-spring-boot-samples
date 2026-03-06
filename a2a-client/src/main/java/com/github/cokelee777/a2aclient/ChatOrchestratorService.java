package com.github.cokelee777.a2aclient;

import lombok.RequiredArgsConstructor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.util.Assert;

@Service
@RequiredArgsConstructor
public class ChatOrchestratorService {

	private final ChatClient chatClient;

	public @Nullable String handleUserQuery(String userMessage) {
		Assert.hasText(userMessage, "문의 내용을 입력해 주세요.");

		try {
			return chatClient.prompt().user(userMessage).call().content();
		}
		catch (Exception e) {
			return "처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
		}
	}

}
