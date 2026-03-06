package com.github.cokelee777.a2aclient;

import org.springframework.util.Assert;

public record ChatResponse(String sessionId, String response) {

	public ChatResponse {
		Assert.hasText(sessionId, "sessionId must not be blank");
		Assert.hasText(response, "response must not be blank");
	}
}
