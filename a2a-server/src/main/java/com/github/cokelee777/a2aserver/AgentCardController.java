package com.github.cokelee777.a2aserver;

import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.spec.AgentCard;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentCardController {

    private final String deliveryAgentCardJson;
    private final String orderAgentCardJson;

    public AgentCardController(
            @Qualifier("deliveryAgentCard") AgentCard deliveryAgentCard,
            @Qualifier("orderAgentCard") AgentCard orderAgentCard
    ) throws JsonProcessingException {
        this.deliveryAgentCardJson = JsonUtil.toJson(deliveryAgentCard);
        this.orderAgentCardJson = JsonUtil.toJson(orderAgentCard);
    }

    @GetMapping(value = "/.well-known/delivery-agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getDeliveryAgentCard() {
        return deliveryAgentCardJson;
    }

    @GetMapping(value = "/.well-known/order-agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getOrderAgentCard() {
        return orderAgentCardJson;
    }
}
