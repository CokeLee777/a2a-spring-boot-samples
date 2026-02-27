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
    public AgentCard orderAgentCard(@Value("${server.port}") int serverPort) {
        return AgentCard.builder()
                .name("Order Cancellability Check Agent")
                .description("주문 취소 가능 여부를 조회하는 에이전트")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                        AgentSkill.builder()
                                .id("order_cancellability_check")
                                .name("주문 취소 가능 여부 조회")
                                .description("주문번호로 주문의 취소 가능 여부를 확인합니다. 배송 및 결제 상태를 종합적으로 체크합니다.")
                                .tags(List.of("order", "cancellability", "check"))
                                .examples(List.of(
                                        "ORD-1001 취소 가능한지 알려줘",
                                        "주문번호 ORD-2002 취소할 수 있어?",
                                        "ORD-3003 주문 취소 가능 여부 확인해줘"
                                ))
                                .build()
                ))
                .supportedInterfaces(List.of(
                        new AgentInterface("JSONRPC", "http://localhost:" + serverPort + "/a2a")))
                .build();
    }
}
