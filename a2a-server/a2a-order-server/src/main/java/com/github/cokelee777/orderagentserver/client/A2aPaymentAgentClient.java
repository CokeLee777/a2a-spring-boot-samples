package com.github.cokelee777.orderagentserver.client;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

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
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * A2A 프로토콜을 사용해 결제 에이전트에 메시지를 보내 환불 가능 여부를 조회합니다.
 */
@Slf4j
@Component
public class A2aPaymentAgentClient {

    private static final Pattern REFUND_ELIGIBLE_LINE = Pattern.compile("refundEligible:(true|false)");

    private final String paymentAgentBaseUrl;
    private final String paymentAgentCardPath = "/.well-known/payment-agent-card.json";

    @Value("${a2a.client.timeout-seconds:12}")
    private int timeoutSeconds;

    public A2aPaymentAgentClient(
            @Value("${order-agent.payment-agent-url}") String paymentAgentBaseUrl) {
        this.paymentAgentBaseUrl = paymentAgentBaseUrl;
    }

    /**
     * 결제 에이전트에 A2A sendMessage로 해당 주문의 환불 가능 여부를 조회합니다.
     */
    public PaymentStatusResponse getPaymentStatus(String orderNumber) {
        try {
            A2AHttpClient httpClient = A2AHttpClientFactory.create();
            AgentCard agentCard = new A2ACardResolver(httpClient, paymentAgentBaseUrl, null, paymentAgentCardPath)
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
                Message agentMessage = Message.builder()
                        .role(Message.Role.ROLE_AGENT)
                        .parts(List.of(new TextPart(orderNumber)))
                        .build();
                client.sendMessage(agentMessage);
            }

            String responseText = resultFuture.get(timeoutSeconds, TimeUnit.SECONDS);
            if (responseText == null || responseText.isBlank()) {
                return new PaymentStatusResponse(orderNumber, false);
            }
            var m = REFUND_ELIGIBLE_LINE.matcher(responseText.trim());
            if (m.find()) {
                return new PaymentStatusResponse(orderNumber, Boolean.parseBoolean(m.group(1)));
            }
            return new PaymentStatusResponse(orderNumber, false);
        } catch (Exception e) {
            log.error("결제 에이전트 호출 실패 (orderNumber={}): {}", orderNumber, e.getMessage(), e);
            return new PaymentStatusResponse(orderNumber, false);
        }
    }

    public record PaymentStatusResponse(String orderNumber, boolean refundEligible) {}
}
