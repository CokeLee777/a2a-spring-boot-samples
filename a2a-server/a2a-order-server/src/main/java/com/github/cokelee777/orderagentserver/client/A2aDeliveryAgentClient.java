package com.github.cokelee777.orderagentserver.client;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
 * A2A 프로토콜을 사용해 배송 에이전트에 메시지를 보내 배송 상태를 조회합니다.
 */
@Component
public class A2aDeliveryAgentClient {

    private static final String INTERNAL_MESSAGE_PREFIX = "[A2A-INTERNAL] delivery-status ";
    private static final Pattern STATUS_LINE = Pattern.compile("status:(.+)");

    private final String deliveryAgentBaseUrl;
    private final String deliveryAgentCardPath = "/.well-known/delivery-agent-card.json";

    public A2aDeliveryAgentClient(
            @Value("${order-agent.delivery-agent-url}") String deliveryAgentBaseUrl) {
        this.deliveryAgentBaseUrl = deliveryAgentBaseUrl;
    }

    /**
     * 배송 에이전트에 A2A sendMessage로 배송 상태를 조회합니다. 주문 취소 가능 여부 판단에 사용합니다.
     */
    public DeliveryStatusResponse getDeliveryStatus(String trackingNumber) {
        try {
            A2AHttpClient httpClient = A2AHttpClientFactory.create();
            AgentCard agentCard = new A2ACardResolver(httpClient, deliveryAgentBaseUrl, null, deliveryAgentCardPath)
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
            Matcher m = STATUS_LINE.matcher(responseText.trim());
            if (m.find()) {
                return new DeliveryStatusResponse(trackingNumber, m.group(1).trim(), null);
            }
            return null;
        } catch (Exception e) {
            return null;
        }
    }

    public record DeliveryStatusResponse(String trackingNumber, String status, String detail) {}
}
