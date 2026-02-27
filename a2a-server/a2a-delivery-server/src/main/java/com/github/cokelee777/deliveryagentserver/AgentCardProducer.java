package com.github.cokelee777.deliveryagentserver;

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
    public AgentCard deliveryAgentCard(@Value("${server.port:8082}") int serverPort) {
        return AgentCard.builder()
                .name("Delivery Tracking Agent")
                .description("배송 정보를 제공하는 에이전트")
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
                        new AgentInterface("JSONRPC", "http://localhost:" + serverPort + "/a2a")))
                .build();
    }
}
