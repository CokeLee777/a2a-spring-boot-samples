package com.github.cokelee777.deliveryagentserver.client;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.HashMap;
import java.util.Map;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.AgentCard;
import io.a2a.spec.Message;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A2A 프로토콜을 사용해 주문 에이전트에 메시지를 보내 주문 정보를 조회합니다.
 */
@Component
public class A2aOrderAgentClient {

    private static final String INTERNAL_MESSAGE_PREFIX = "[A2A-INTERNAL] order-info ";

    private final String orderAgentBaseUrl;
    private final String orderAgentCardPath = "/.well-known/order-agent-card.json";

    public A2aOrderAgentClient(
            @Value("${delivery-agent.order-agent-url:http://localhost:8082}") String orderAgentBaseUrl) {
        this.orderAgentBaseUrl = orderAgentBaseUrl;
    }

    /**
     * 주문 에이전트에 A2A sendMessage로 운송장번호에 해당하는 주문 정보를 조회합니다.
     */
    public OrderInfoResponse getOrderInfo(String trackingNumber) {
        try {
            A2AHttpClient httpClient = A2AHttpClientFactory.create();
            AgentCard agentCard = new A2ACardResolver(httpClient, orderAgentBaseUrl, null, orderAgentCardPath)
                    .getAgentCard();

            CompletableFuture<String> resultFuture = new CompletableFuture<>();

            List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of(
                    (event, card) -> {
                        if (event instanceof TaskEvent taskEvent) {
                            Task task = taskEvent.getTask();
                            StringBuilder sb = new StringBuilder();
                            if (task.artifacts() != null) {
                                task.artifacts().forEach(artifact ->
                                        artifact.parts().forEach(part -> {
                                            if (part instanceof TextPart textPart) {
                                                sb.append(textPart.text());
                                            }
                                        })
                                );
                            }
                            resultFuture.complete(sb.toString());
                        }
                    }
            );

            ClientConfig clientConfig = new ClientConfig.Builder()
                    .setAcceptedOutputModes(List.of("text"))
                    .build();

            try (Client client = Client
                    .builder(agentCard)
                    .clientConfig(clientConfig)
                    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                    .addConsumers(consumers)
                    .build()) {
                String internalMessage = INTERNAL_MESSAGE_PREFIX + trackingNumber;
                Message userMessage = Message.builder()
                        .role(Message.Role.ROLE_USER)
                        .parts(List.of(new TextPart(internalMessage)))
                        .build();
                client.sendMessage(userMessage);
            }

            String responseText = resultFuture.get(10, TimeUnit.SECONDS);
            if (responseText == null || responseText.isBlank()) {
                return null;
            }
            Map<String, String> parsed = new HashMap<>();
            for (String line : responseText.split("\n")) {
                int colon = line.indexOf(':');
                if (colon > 0) {
                    parsed.put(line.substring(0, colon).trim(), line.substring(colon + 1).trim());
                }
            }
            String orderNumber = parsed.get("orderNumber");
            String orderDate = parsed.get("orderDate");
            String status = parsed.get("status");
            if (orderNumber == null || "NOT_FOUND".equals(orderNumber)) {
                return null;
            }
            return new OrderInfoResponse(orderNumber, parsed.get("productName"), status, orderDate, trackingNumber);
        } catch (Exception e) {
            return null;
        }
    }

    public record OrderInfoResponse(
            String orderNumber,
            String productName,
            String status,
            String orderDate,
            String trackingNumber
    ) {}
}
