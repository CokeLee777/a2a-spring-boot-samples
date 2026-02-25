package com.github.cokelee777.orderagentserver;

import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.spec.AgentCard;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AgentCardController {

    private final String orderAgentCardJson;

    public AgentCardController(AgentCard orderAgentCard) throws JsonProcessingException {
        this.orderAgentCardJson = JsonUtil.toJson(orderAgentCard);
    }

    @GetMapping(value = "/.well-known/order-agent-card.json", produces = MediaType.APPLICATION_JSON_VALUE)
    public String getOrderAgentCard() {
        return orderAgentCardJson;
    }
}
