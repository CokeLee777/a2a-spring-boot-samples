package com.github.cokelee777.common.server;

import io.a2a.spec.AgentCard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for serving the agent card at the RFC 8615 well-known endpoint.
 *
 * <p>
 * Each server module provides an {@link AgentCard} bean; this controller serves it at
 * {@code /.well-known/agent-card.json}.
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class A2AAgentCardController {

	private final AgentCard agentCard;

	/**
	 * Returns the agent card describing this agent's capabilities and skills.
	 * @return a {@code ResponseEntity} containing the {@link AgentCard}
	 */
	@GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AgentCard> getAgentCard() {
		return ResponseEntity.ok(agentCard);
	}

}
