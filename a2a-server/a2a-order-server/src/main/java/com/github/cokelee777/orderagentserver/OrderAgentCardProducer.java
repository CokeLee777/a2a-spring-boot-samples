package com.github.cokelee777.orderagentserver;

import io.a2a.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration bean producer for the order agent's card.
 * <p>
 * This class creates and configures the {@code AgentCard} that describes the order
 * agent's capabilities, endpoints, and available skills.
 * </p>
 */
@Configuration
public class OrderAgentCardProducer {

	/**
	 * Produces the order agent card bean.
	 * <p>
	 * The card describes:
	 * </p>
	 * <ul>
	 * <li>Agent name: "Order Agent"</li>
	 * <li>Available skills: order_list and order_cancellability_check</li>
	 * <li>Communication protocol: JSON-RPC</li>
	 * <li>Supported I/O modes: text</li>
	 * </ul>
	 * @param serverPort the port on which this agent listens
	 * @return a configured {@code AgentCard} instance
	 */
	@Bean
	public AgentCard agentCard(@Value("${server.port}") int serverPort) {
		final String AGENT_URL = String.format("http://localhost:%s/a2a", serverPort);
		return AgentCard.builder()
			.name("Order Agent")
			.description("주문 내역 조회 및 주문 취소 가능 여부를 조회하는 에이전트")
			.supportedInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), AGENT_URL)))
			.version("1.0.0")
			.capabilities(AgentCapabilities.builder().streaming(false).pushNotifications(false).build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of(
					AgentSkill.builder()
						.id("order_list")
						.name("주문 내역 조회")
						.description("회원 ID로 해당 회원의 주문 목록을 조회합니다. MEMBER-{memberId} 형식으로 호출합니다.")
						.tags(List.of("order", "list", "history"))
						.examples(List.of("MEMBER-user1 주문내역 조회"))
						.build(),
					AgentSkill.builder()
						.id("order_cancellability_check")
						.name("주문 취소 가능 여부 조회")
						.description("주문번호로 주문의 취소 가능 여부를 확인합니다. 배송 및 결제 상태를 종합적으로 체크합니다.")
						.tags(List.of("order", "cancellability", "check"))
						.examples(
								List.of("ORD-1001 취소 가능한지 알려줘", "주문번호 ORD-2002 취소할 수 있어?", "ORD-3003 주문 취소 가능 여부 확인해줘"))
						.build(),
					AgentSkill.builder()
						.id("order_info_by_tracking")
						.name("운송장 번호로 주문 정보 조회 (내부)")
						.description("운송장 번호로 주문 정보를 조회합니다. 배송 에이전트의 내부 A2A 호출 전용입니다.")
						.tags(List.of("order", "tracking", "internal"))
						.examples(List.of("TRACK-1001"))
						.build()))
			.build();
	}

}
