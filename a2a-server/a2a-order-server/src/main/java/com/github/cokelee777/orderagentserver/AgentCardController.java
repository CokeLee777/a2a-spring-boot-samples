package com.github.cokelee777.orderagentserver;

import io.a2a.spec.AgentCard;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class AgentCardController {

    private final AgentCard orderAgentCard;

    @GetMapping(value = "/.well-known/order-agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<AgentCard> getOrderAgentCard() {
        return ResponseEntity.ok(orderAgentCard);
    }
}
