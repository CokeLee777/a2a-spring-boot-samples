package com.github.cokelee777.deliveryagentserver.client;

import io.a2a.A2A;
import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

/**
 * A2A 프로토콜을 사용해 주문 에이전트에 메시지를 보내 주문 정보를 조회합니다.
 */
@Slf4j
@Component
public class A2aOrderAgentClient {

    private final String orderAgentBaseUrl;
    private volatile AgentCard orderAgentCard;

    @Value("${a2a.client.timeout-seconds}")
    private int timeoutSeconds;

    public A2aOrderAgentClient(
            @Value("${delivery-agent.order-agent-url}") String orderAgentBaseUrl) {
        this.orderAgentBaseUrl = orderAgentBaseUrl;
    }

    private AgentCard resolveAgentCard() {
        if (orderAgentCard == null) {
            synchronized (this) {
                if (orderAgentCard == null) {
                    A2AHttpClient httpClient = A2AHttpClientFactory.create();
                    orderAgentCard = new A2ACardResolver(httpClient, orderAgentBaseUrl, null).getAgentCard();
                    log.info("Order agent card resolved: {}", orderAgentCard.name());
                }
            }
        }
        return orderAgentCard;
    }

    /**
     * 주문 에이전트에 A2A sendMessage로 운송장번호에 해당하는 주문 정보를 조회합니다.
     */
    public OrderInfoResponse getOrderInfo(String trackingNumber) {
        try {
            ClientConfig clientConfig = new ClientConfig.Builder()
                    .setAcceptedOutputModes(List.of("text"))
                    .build();

            CompletableFuture<String> resultFuture = new CompletableFuture<>();
            List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of(
                    (event, card) -> {
                        if (event instanceof TaskEvent taskEvent) {
                            Task task = taskEvent.getTask();
                            if (TaskState.TASK_STATE_FAILED.equals(task.status().state())) {
                                resultFuture.complete(null);
                                return;
                            }
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

            try (Client client = Client
                    .builder(resolveAgentCard())
                    .clientConfig(clientConfig)
                    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                    .addConsumers(consumers)
                    .build()) {
                Message message = A2A.toAgentMessage(trackingNumber);
                client.sendMessage(message);
            }

            String responseText = resultFuture.get(timeoutSeconds, TimeUnit.SECONDS);
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
            log.error("주문 에이전트 호출 실패 (trackingNumber={}): {}", trackingNumber, e.getMessage(), e);
            return null;
        }
    }

    public record OrderInfoResponse(
            String orderNumber,
            String productName,
            String status,
            String orderDate,
            String trackingNumber
    ) {
    }
}
