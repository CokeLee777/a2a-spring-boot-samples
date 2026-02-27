package com.github.cokelee777.paymentagentserver;

import java.util.List;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentCardProducer {

    @Bean
    public AgentCard paymentAgentCard(@Value("${server.port:8083}") int serverPort) {
        return AgentCard.builder()
                .name("Payment Agent")
                .description("결제·환불 상태를 조회하고, 주문 취소 시 환불 가능 여부를 판단하는 에이전트")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                        AgentSkill.builder()
                                .id("payment_status")
                                .name("결제/환불 상태 조회")
                                .description("주문번호 기준 결제 상태 및 환불 가능 여부를 조회합니다.")
                                .tags(List.of("payment", "refund", "order"))
                                .examples(List.of(
                                        "[A2A-INTERNAL] payment-status ORD-1001"
                                ))
                                .build()
                ))
                .supportedInterfaces(List.of(
                        new AgentInterface("JSONRPC", "http://localhost:" + serverPort + "/a2a")))
                .build();
    }
}
