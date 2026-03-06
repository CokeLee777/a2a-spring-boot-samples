package com.github.cokelee777.a2aclient;

import jakarta.validation.constraints.NotBlank;

import java.util.Objects;
import java.util.UUID;

/**
 * 생성 후에는 {@link #sessionId()}가 항상 non-null입니다 (입력이 null이면 새 UUID로 대체).
 * {@link #memberId()}가 있으면 해당 회원의 주문내역 조회·취소 가능 여부 등을 맥락으로 사용합니다.
 */
public record ChatRequest(String sessionId, @NotBlank(message = "memberId must not be blank") String memberId,
		@NotBlank(message = "message must not be blank") String message) {

	public ChatRequest {
		sessionId = Objects.requireNonNullElse(sessionId, UUID.randomUUID().toString());
	}
}
