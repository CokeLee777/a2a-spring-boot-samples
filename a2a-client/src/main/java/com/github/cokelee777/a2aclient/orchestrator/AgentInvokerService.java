package com.github.cokelee777.a2aclient.orchestrator;

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
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

@Slf4j
@Service
public class AgentInvokerService {

    private final String orderAgentUrl;
    private final String deliveryAgentUrl;
    private volatile AgentCard orderAgentCard;
    private volatile AgentCard deliveryAgentCard;

    @Value("${a2a.client.timeout-seconds:15}")
    private int timeoutSeconds;

    public AgentInvokerService(
            @Value("${a2a.order-agent-url}") String orderAgentUrl,
            @Value("${a2a.delivery-agent-url}") String deliveryAgentUrl) {
        this.orderAgentUrl = orderAgentUrl;
        this.deliveryAgentUrl = deliveryAgentUrl;
    }

    /**
     * 주문 에이전트에 주문 취소 가능 여부를 확인하는 요청을 보낸다.
     *
     * @param orderNumber 취소 가능 여부를 확인할 주문 번호
     * @return 주문 에이전트로부터 반환된 응답 문자열
     */
    public String callOrderAgent(String orderNumber) {
        String messageToSend = String.format("%s 취소 가능 여부 확인", orderNumber);
        return sendRequest(resolveOrderAgentCard(), messageToSend);
    }

    /**
     * 배송 에이전트에 배송 조회 요청을 보낸다.
     *
     * @param trackingNumber 조회할 운송장 번호
     * @return 배송 에이전트로부터 반환된 응답 문자열
     */
    public String callDeliveryAgent(String trackingNumber) {
        String messageToSend = String.format("%s 배송 조회", trackingNumber);
        return sendRequest(resolveDeliveryAgentCard(), messageToSend);
    }

    private AgentCard resolveOrderAgentCard() {
        if (orderAgentCard == null) {
            synchronized (this) {
                if (orderAgentCard == null) {
                    A2AHttpClient httpClient = A2AHttpClientFactory.create();
                    orderAgentCard = new A2ACardResolver(httpClient, orderAgentUrl, null).getAgentCard();
                    log.info("Order agent card resolved: {}", orderAgentCard.name());
                }
            }
        }
        return orderAgentCard;
    }

    private AgentCard resolveDeliveryAgentCard() {
        if (deliveryAgentCard == null) {
            synchronized (this) {
                if (deliveryAgentCard == null) {
                    A2AHttpClient httpClient = A2AHttpClientFactory.create();
                    deliveryAgentCard = new A2ACardResolver(httpClient, deliveryAgentUrl, null).getAgentCard();
                    log.info("Delivery agent card resolved: {}", deliveryAgentCard.name());
                }
            }
        }
        return deliveryAgentCard;
    }

    private String sendRequest(AgentCard agentCard, String text) {
        try {
            CompletableFuture<String> resultFuture = new CompletableFuture<>();

            List<BiConsumer<ClientEvent, AgentCard>> consumers = List.of(
                    (event, card) -> {
                        if (event instanceof TaskEvent taskEvent) {
                            Task task = taskEvent.getTask();
                            if (TaskState.TASK_STATE_FAILED.equals(task.status().state())) {
                                resultFuture.complete("처리 중 오류가 발생했습니다.");
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

            Consumer<Throwable> errorHandler = resultFuture::completeExceptionally;

            ClientConfig clientConfig = new ClientConfig.Builder()
                    .setAcceptedOutputModes(List.of(TextPart.TEXT))
                    .build();

            try (Client client = Client
                    .builder(agentCard)
                    .clientConfig(clientConfig)
                    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
                    .addConsumers(consumers)
                    .streamingErrorHandler(errorHandler)
                    .build()) {
                Message userMessage = Message.builder()
                        .role(Message.Role.ROLE_USER)
                        .parts(List.of(new TextPart(text)))
                        .build();
                client.sendMessage(userMessage);
            }

            return resultFuture.get(timeoutSeconds, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "에이전트 호출 중 오류: " + e.getMessage();
        }
    }
}
