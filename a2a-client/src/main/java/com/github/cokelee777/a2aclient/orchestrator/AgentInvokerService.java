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

    private static final String DELIVERY_AGENT_CARD_PATH = "/.well-known/delivery-agent-card.json";
    private static final String ORDER_AGENT_CARD_PATH = "/.well-known/order-agent-card.json";

    private final String orderAgentUrl;
    private final String deliveryAgentUrl;

    public AgentInvokerService(
            @Value("${a2a.order-agent-url:http://localhost:8082}") String orderAgentUrl,
            @Value("${a2a.delivery-agent-url:http://localhost:8083}") String deliveryAgentUrl) {
        this.orderAgentUrl = orderAgentUrl;
        this.deliveryAgentUrl = deliveryAgentUrl;
    }

    public String callOrderAgent(String messageToSend) {
        return sendRequest(orderAgentUrl, ORDER_AGENT_CARD_PATH, messageToSend);
    }

    public String callDeliveryAgent(String messageToSend) {
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
