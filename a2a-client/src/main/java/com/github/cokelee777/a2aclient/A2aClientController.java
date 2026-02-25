package com.github.cokelee777.a2aclient;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import io.a2a.client.Client;
import io.a2a.client.ClientEvent;
import io.a2a.client.MessageEvent;
import io.a2a.client.TaskEvent;
import io.a2a.client.TaskUpdateEvent;
import io.a2a.client.config.ClientConfig;
import io.a2a.client.http.A2ACardResolver;
import io.a2a.client.http.A2AHttpClient;
import io.a2a.client.http.A2AHttpClientFactory;
import io.a2a.client.transport.jsonrpc.JSONRPCTransport;
import io.a2a.client.transport.jsonrpc.JSONRPCTransportConfig;
import io.a2a.spec.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class A2aClientController {

    private static final String SERVER_URL = "http://localhost:8082";
    private static final String DELIVERY_AGENT_CARD_PATH = "/.well-known/delivery-agent-card.json";
    private static final String ORDER_AGENT_CARD_PATH = "/.well-known/order-agent-card.json";

    @GetMapping("/api/delivery")
    public String trackDelivery(@RequestParam String trackingNumber) {
        return sendRequest(DELIVERY_AGENT_CARD_PATH, trackingNumber + " 배송 조회해줘");
    }

    @GetMapping("/api/order/cancel")
    public String cancelOrder(@RequestParam String orderNumber) {
        return sendRequest(ORDER_AGENT_CARD_PATH, orderNumber + " 주문 취소해줘");
    }

    private String sendRequest(String agentCardPath, String text) {
        try {
            A2AHttpClient httpClient = A2AHttpClientFactory.create();
            AgentCard agentCard = new A2ACardResolver(httpClient, SERVER_URL, null, agentCardPath)
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
                        } else if (event instanceof MessageEvent messageEvent) {
                            resultFuture.complete("Message: " + messageEvent.getMessage());
                        } else if (event instanceof TaskUpdateEvent taskUpdateEvent) {
                            resultFuture.complete("TaskUpdate: " + taskUpdateEvent.getTask());
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
            } catch (A2AClientException e) {
                throw e;
            }

            return resultFuture.get(10, TimeUnit.SECONDS);
        } catch (Exception e) {
            return "Error: " + e.getMessage();
        }
    }
}
