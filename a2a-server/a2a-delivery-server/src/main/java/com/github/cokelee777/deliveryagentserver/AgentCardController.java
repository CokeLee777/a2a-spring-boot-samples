package com.github.cokelee777.deliveryagentserver;

import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.spec.AgentCard;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentCardController {

    private final String deliveryAgentCardJson;

    public AgentCardController(AgentCard deliveryAgentCard) throws JsonProcessingException {
        this.deliveryAgentCardJson = JsonUtil.toJson(deliveryAgentCard);
    }

    @GetMapping(value = "/.well-known/delivery-agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getDeliveryAgentCard() {
        return deliveryAgentCardJson;
    }
}
