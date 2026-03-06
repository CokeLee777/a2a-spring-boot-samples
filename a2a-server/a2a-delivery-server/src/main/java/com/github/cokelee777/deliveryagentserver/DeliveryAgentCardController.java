package com.github.cokelee777.deliveryagentserver;

import io.a2a.spec.AgentCard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for serving the delivery agent's card via A2A Protocol.
 * <p>
 * This controller provides the agent card at the RFC 8615 well-known endpoint, which
 * describes the agent's capabilities, supported interfaces, and available skills.
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class DeliveryAgentCardController {

	private final AgentCard deliveryAgentCard;

	/**
	 * Retrieves the delivery agent card.
	 * @return a {@code ResponseEntity} containing the {@code AgentCard}
	 */
	@GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AgentCard> getDeliveryAgentCard() {
		return ResponseEntity.ok(deliveryAgentCard);
	}

}
