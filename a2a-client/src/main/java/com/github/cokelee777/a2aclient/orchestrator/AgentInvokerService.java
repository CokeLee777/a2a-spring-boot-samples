package com.github.cokelee777.a2aclient.orchestrator;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

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
import org.springframework.stereotype.Service;

@Service
public class AgentInvokerService {

    private static final String ORDER_AGENT_CARD_PATH = "/.well-known/order-agent-card.json";
    private static final String DELIVERY_AGENT_CARD_PATH = "/.well-known/delivery-agent-card.json";

    private final String orderAgentUrl;
    private final String deliveryAgentUrl;

    public AgentInvokerService(
            @Value("${a2a.order-agent-url}") String orderAgentUrl,
            @Value("${a2a.delivery-agent-url}") String deliveryAgentUrl) {
        this.orderAgentUrl = orderAgentUrl;
        this.deliveryAgentUrl = deliveryAgentUrl;
    }

    /**
     * 주문 에이전트에 주문 취소 가능 여부를 확인하는 요청을 보낸다.
     *
     * <p>
     * 전달받은 주문번호를 기반으로 "취소 가능 여부 확인" 메시지를 생성하여
     * Order Agent(A2A 서버)에 요청을 전송한다.
     * </p>
     *
     * @param orderNumber 취소 가능 여부를 확인할 주문 번호
     * @return 주문 에이전트로부터 반환된 응답 문자열
     */
    public String callOrderAgent(String orderNumber) {
        String messageToSend = String.format("%s 취소 가능 여부 확인", orderNumber);
        return sendRequest(orderAgentUrl, ORDER_AGENT_CARD_PATH, messageToSend);
    }

    /**
     * 배송 에이전트에 배송 조회 요청을 보낸다.
     *
     * <p>
     * 전달받은 운송장 번호를 기반으로 "배송 조회" 메시지를 생성하여
     * Delivery Agent(A2A 서버)에 요청을 전송한다.
     * </p>
     *
     * @param trackingNumber 조회할 운송장 번호
     * @return 배송 에이전트로부터 반환된 응답 문자열
     */
    public String callDeliveryAgent(String trackingNumber) {
        String messageToSend = String.format("%s 배송 조회", trackingNumber);
        return sendRequest(deliveryAgentUrl, DELIVERY_AGENT_CARD_PATH, messageToSend);
    }

    private String sendRequest(String serverUrl, String agentCardPath, String text) {
        try {
            A2AHttpClient httpClient = A2AHttpClientFactory.create();
            AgentCard agentCard = new A2ACardResolver(httpClient, serverUrl, null, agentCardPath)
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

            Consumer<Throwable> errorHandler = resultFuture::completeExceptionally;

            ClientConfig clientConfig = new ClientConfig.Builder()
                    .setAcceptedOutputModes(List.of("text"))
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

            return resultFuture.get(15, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "에이전트 호출 중 오류: " + e.getMessage();
        }
    }
}
