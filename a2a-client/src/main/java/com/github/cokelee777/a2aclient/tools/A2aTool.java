package com.github.cokelee777.a2aclient.tools;

import com.github.cokelee777.a2a.common.transport.A2aTransport;
import io.a2a.A2A;
import org.springframework.beans.factory.annotation.Value;

/**
 * Abstract base class for A2A (Agent-to-Agent) protocol tools.
 *
 * <p>
 * Provides {@link #sendRequest(String)} which delegates all transport concerns (AgentCard
 * caching, CompletableFuture handling, timeout) to {@link A2aTransport}. Subclasses
 * implement specific agent operations using the {@code @Tool} annotation from Spring AI.
 * </p>
 */
public abstract class A2aTool {

	private final A2aTransport transport;

	@Value("${a2a.client.timeout-seconds}")
	private int timeoutSeconds;

	/**
	 * Constructs an A2aTool targeting the given agent URL.
	 * @param agentUrl the base URL of the target agent
	 */
	protected A2aTool(String agentUrl) {
		this.transport = new A2aTransport(agentUrl);
	}

	/**
	 * Sends a user-role message to the remote agent and returns the response text.
	 *
	 * <p>
	 * Returns a Korean error message if the agent call fails or times out.
	 * </p>
	 * @param text the message text to send
	 * @return the agent response, or a fallback error message
	 */
	protected String sendRequest(String text) {
		return transport.send(A2A.toUserMessage(text), timeoutSeconds).orElse("에이전트 호출 중 오류가 발생했습니다.");
	}

}
