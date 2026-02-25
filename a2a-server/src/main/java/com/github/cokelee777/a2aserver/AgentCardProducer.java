package com.github.cokelee777.a2aserver;

import java.util.List;

import io.a2a.spec.AgentCapabilities;
import io.a2a.spec.AgentCard;
import io.a2a.spec.AgentInterface;
import io.a2a.spec.AgentSkill;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AgentCardProducer {

    @Bean
    public AgentCard deliveryAgentCard() {
        return AgentCard.builder()
                .name("Delivery Tracking Agent")
                .description("배송 상태를 조회하고 실시간 배송 정보를 제공하는 에이전트")
                .version("1.0.0")
                .capabilities(AgentCapabilities.builder()
                        .streaming(false)
                        .pushNotifications(false)
                        .build())
                .defaultInputModes(List.of("text"))
                .defaultOutputModes(List.of("text"))
                .skills(List.of(
                        AgentSkill.builder()
                                .id("track_delivery")
                                .name("배송 조회")
                                .description("운송장 번호로 현재 배송 상태를 조회합니다")
                                .tags(List.of("delivery", "tracking", "shipping"))
                                .examples(List.of(
                                        "TRACK-1001 배송 조회해줘",
                                        "운송장번호 TRACK-2002 어디까지 왔어?",
                                        "TRACK-3003 배송 상태 알려줘"
                                ))
                                .build()
                ))
                .supportedInterfaces(List.of(
                        new AgentInterface("JSONRPC", "http://localhost:8082/a2a")))
                .build();
    }

    @Bean
    public AgentCard orderAgentCard() {
        return AgentCard.builder()
                .name("Order Cancellation Agent")
                .description("주문 취소를 처리하는 에이전트")
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
                        new AgentInterface("JSONRPC", "http://localhost:8082/a2a")))
                .build();
    }
}
