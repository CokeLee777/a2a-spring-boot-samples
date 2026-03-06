package com.github.cokelee777.paymentagentserver;

import io.a2a.spec.AgentCard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for exposing the payment agent's agent card.
 * <p>
 * This controller serves the agent card at the RFC 8615 standard well-known location,
 * allowing other agents to discover this agent's capabilities and interface.
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class PaymentAgentCardController {

	private final AgentCard paymentAgentCard;

	/**
	 * Retrieves the payment agent's agent card.
	 * <p>
	 * The agent card describes the payment agent's name, description, supported
	 * interfaces, capabilities, and available skills.
	 * </p>
	 * @return a ResponseEntity containing the payment agent card
	 */
	@GetMapping(value = "/.well-known/agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<AgentCard> getPaymentAgentCard() {
		return ResponseEntity.ok(paymentAgentCard);
	}

}
