package com.github.cokelee777.a2aclient.tools;

import com.github.cokelee777.a2a.common.metadata.A2aMetadataKeys;
import com.github.cokelee777.a2a.common.transport.A2aTransport;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import org.springframework.beans.factory.annotation.Value;

import java.util.List;
import java.util.Map;

/**
 * Abstract base class for A2A (Agent-to-Agent) protocol tools.
 *
 * <p>
 * Provides {@link #sendRequest(String, String)} which builds an A2A message with the
 * given skill ID in metadata and delegates transport concerns to {@link A2aTransport}.
 * Subclasses implement specific agent operations using the {@code @Tool} annotation from
 * Spring AI.
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
	 * Sends a user-role message with the given skill ID to the remote agent.
	 *
	 * <p>
	 * The skill ID is carried in the message metadata under the key
	 * {@link A2aMetadataKeys#SKILL_ID}. Returns a Korean error message if the agent call
	 * fails or times out.
	 * </p>
	 * @param skillId the skill ID declared in the target agent's AgentCard
	 * @param text the message text to send
	 * @return the agent response, or a fallback error message
	 */
	protected String sendRequest(String skillId, String text) {
		Message message = Message.builder()
			.role(Message.Role.ROLE_USER)
			.parts(List.of(new TextPart(text)))
			.metadata(Map.of(A2aMetadataKeys.SKILL_ID, skillId))
			.build();
		return transport.send(message, timeoutSeconds).orElse("에이전트 호출 중 오류가 발생했습니다.");
	}

}
