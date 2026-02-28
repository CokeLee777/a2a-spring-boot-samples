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
import io.a2a.spec.TaskState;
import io.a2a.spec.TextPart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A2A 프로토콜을 사용해 배송 에이전트에 메시지를 보내 배송 상태를 조회합니다.
 */
@Slf4j
@Component
public class A2aDeliveryAgentClient {

    private static final Pattern STATUS_LINE = Pattern.compile("status:(.+)");

    private final String deliveryAgentBaseUrl;
    private volatile AgentCard deliveryAgentCard;

    @Value("${a2a.client.timeout-seconds}")
    private int timeoutSeconds;

    public A2aDeliveryAgentClient(
            @Value("${order-agent.delivery-agent-url}") String deliveryAgentBaseUrl) {
        this.deliveryAgentBaseUrl = deliveryAgentBaseUrl;
    }

    private AgentCard resolveAgentCard() {
        if (deliveryAgentCard == null) {
            synchronized (this) {
                if (deliveryAgentCard == null) {
                    A2AHttpClient httpClient = A2AHttpClientFactory.create();
                    deliveryAgentCard = new A2ACardResolver(httpClient, deliveryAgentBaseUrl, null).getAgentCard();
                    log.info("Delivery agent card resolved: {}", deliveryAgentCard.name());
                }
            }
        }
        return deliveryAgentCard;
    }

    /**
     * 배송 에이전트에 A2A sendMessage로 배송 상태를 조회합니다. 주문 취소 가능 여부 판단에 사용합니다.
     */
    public DeliveryStatusResponse getDeliveryStatus(String trackingNumber) {
        try {
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

            ClientConfig clientConfig = new ClientConfig.Builder()
                    .setAcceptedOutputModes(List.of(TextPart.TEXT))
                    .build();

            try (Client client = Client
                    .builder(resolveAgentCard())
                    .clientConfig(clientConfig)
                    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                    .addConsumers(consumers)
                    .build()) {
                Message agentMessage = Message.builder()
                        .role(Message.Role.ROLE_AGENT)
                        .parts(List.of(new TextPart(trackingNumber)))
                        .build();
                client.sendMessage(agentMessage);
            }

            String responseText = resultFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            if (responseText == null || responseText.isBlank()) {
                return null;
            }
            Matcher m = STATUS_LINE.matcher(responseText.trim());
            if (m.find()) {
                return new DeliveryStatusResponse(trackingNumber, m.group(1).trim(), null);
            }
            return null;
        } catch (Exception e) {
            log.error("배송 에이전트 호출 실패 (trackingNumber={}): {}", trackingNumber, e.getMessage(), e);
            return null;
        }
    }

    public record DeliveryStatusResponse(String trackingNumber, String status, String detail) {}
}
