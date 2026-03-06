package com.github.cokelee777.a2aclient;

import jakarta.validation.constraints.NotBlank;

import java.util.Objects;
import java.util.UUID;

/**
 * 생성 후에는 {@link #sessionId()}가 항상 non-null입니다 (입력이 null이면 새 UUID로 대체).
 */
public record ChatRequest(String sessionId, @NotBlank(message = "문의 내용을 입력해 주세요.") String message) {

	public ChatRequest {
		sessionId = Objects.requireNonNullElse(sessionId, UUID.randomUUID().toString());
	}
}
