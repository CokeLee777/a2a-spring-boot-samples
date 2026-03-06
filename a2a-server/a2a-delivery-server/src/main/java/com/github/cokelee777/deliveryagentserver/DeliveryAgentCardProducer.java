package com.github.cokelee777.deliveryagentserver;

import io.a2a.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;
import java.util.List;

/**
 * Configuration bean producer for the delivery agent's card.
 * <p>
 * This class creates and configures the {@code AgentCard} that describes the delivery
 * agent's capabilities, endpoints, and available skills.
 * </p>
 */
@Configuration
public class DeliveryAgentCardProducer {

	/**
	 * Produces the delivery agent card bean.
	 * <p>
	 * The card describes:
	 * </p>
	 * <ul>
	 * <li>Agent name: "Delivery Tracking Agent"</li>
	 * <li>Available skill: track_delivery (shipping status lookup)</li>
	 * <li>Communication protocol: JSON-RPC</li>
	 * <li>Supported I/O modes: text</li>
	 * </ul>
	 * @param serverPort the port on which this agent listens
	 * @return a configured {@code AgentCard} instance
	 */
	@Bean
	public AgentCard deliveryAgentCard(@Value("${server.port}") int serverPort) {
		final String AGENT_URL = String.format("http://localhost:%s/a2a", serverPort);
		return AgentCard.builder()
			.name("Delivery Tracking Agent")
			.description("배송 정보를 제공하는 에이전트")
			.supportedInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), AGENT_URL)))
			.version("1.0.0")
			.capabilities(AgentCapabilities.builder().streaming(false).pushNotifications(false).build())
			.defaultInputModes(Collections.singletonList("text"))
			.defaultOutputModes(Collections.singletonList("text"))
			.skills(List.of(AgentSkill.builder()
				.id("track_delivery")
				.name("배송 조회")
				.description("운송장 번호로 현재 배송 상태를 조회합니다")
				.tags(List.of("delivery", "tracking", "shipping"))
				.examples(List.of("TRACK-1001 배송 조회해줘", "운송장번호 TRACK-2002 어디까지 왔어?", "TRACK-3003 배송 상태 알려줘"))
				.build()))
			.build();
	}

}
