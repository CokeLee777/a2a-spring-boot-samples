package com.github.cokelee777.paymentagentserver;

import io.a2a.spec.AgentCard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AgentCardController {

    private final AgentCard paymentAgentCard;

    @GetMapping(value = "/.well-known/payment-agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AgentCard> getPaymentAgentCard() {
        return ResponseEntity.ok(paymentAgentCard);
    }
}
