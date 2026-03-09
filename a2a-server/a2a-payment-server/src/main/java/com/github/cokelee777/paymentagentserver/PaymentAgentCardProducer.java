package com.github.cokelee777.paymentagentserver;

import io.a2a.spec.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration class for producing the payment agent's agent card.
 * <p>
 * This class creates and configures the AgentCard bean that describes the payment agent's
 * capabilities, interfaces, and available skills.
 * </p>
 */
@Configuration
public class PaymentAgentCardProducer {

	/**
	 * Produces the agent card for the payment agent.
	 * <p>
	 * Creates an AgentCard with the payment agent's metadata including name, description,
	 * supported interfaces, and available skills.
	 * </p>
	 * @param serverPort the port on which this agent is running
	 * @return the configured AgentCard for the payment agent
	 */
	@Bean
	public AgentCard agentCard(@Value("${server.port}") int serverPort) {
		final String AGENT_URL = String.format("http://localhost:%s/a2a", serverPort);
		return AgentCard.builder()
			.name("Payment Agent")
			.description("결제·환불 상태를 조회하고, 주문 취소 시 환불 가능 여부를 판단하는 에이전트")
			.supportedInterfaces(List.of(new AgentInterface(TransportProtocol.JSONRPC.asString(), AGENT_URL)))
			.version("1.0.0")
			.capabilities(AgentCapabilities.builder().streaming(false).pushNotifications(false).build())
			.defaultInputModes(List.of("text"))
			.defaultOutputModes(List.of("text"))
			.skills(List.of(AgentSkill.builder()
				.id(PaymentAgentSkillIds.PAYMENT_STATUS)
				.name("결제/환불 상태 조회 (내부)")
				.description("주문번호 기준 결제 상태 및 환불 가능 여부를 조회합니다. 주문 에이전트의 내부 A2A 호출 전용입니다.")
				.tags(List.of("payment", "refund", "order"))
				.examples(List.of("ORD-1001"))
				.build()))
			.build();
	}

}
