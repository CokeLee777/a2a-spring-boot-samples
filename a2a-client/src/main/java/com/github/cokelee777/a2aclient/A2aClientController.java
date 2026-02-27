package com.github.cokelee777.a2aclient;

import com.github.cokelee777.a2aclient.orchestrator.AgentInvokerService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class A2aClientController {

    private final AgentInvokerService agentInvokerService;

    public A2aClientController(AgentInvokerService agentInvokerService) {
        this.agentInvokerService = agentInvokerService;
    }

    @GetMapping("/api/delivery")
    public String trackDelivery(@RequestParam String trackingNumber) {
        return agentInvokerService.callDeliveryAgent(trackingNumber);
    }

    @GetMapping("/api/order/cancel")
    public String cancelOrder(@RequestParam String orderNumber) {
        return agentInvokerService.callOrderAgent(orderNumber);
    }
}
