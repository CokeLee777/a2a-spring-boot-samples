package com.github.cokelee777.orderagentserver;

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
    public AgentCard orderAgentCard(@Value("${server.port:8082}") int serverPort) {
        return AgentCard.builder()
                .name("Order Cancellation Agent")
                .description("주문 취소를 처리하는 에이전트. 배송 에이전트와 연동하여 배송 상태를 확인합니다.")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                        AgentSkill.builder()
                                .id("cancel_order")
                                .name("주문 취소")
                                .description("주문번호로 주문을 취소합니다. 배송중/배송완료 상태에서는 취소할 수 없습니다.")
                                .tags(List.of("order", "cancel", "refund"))
                                .examples(List.of(
                                        "ORD-1001 주문 취소해줘",
                                        "주문번호 ORD-2002 취소하고 싶어",
                                        "ORD-3003 주문취소 가능해?"
                                ))
                                .build()
                ))
                .supportedInterfaces(List.of(
                        new AgentInterface("JSONRPC", "http://localhost:" + serverPort + "/a2a")))
                .build();
    }
}
