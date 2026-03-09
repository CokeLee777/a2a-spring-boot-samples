package com.github.cokelee777.orderagentserver.client;

import com.github.cokelee777.a2a.common.metadata.A2aMetadataKeys;
import com.github.cokelee777.a2a.common.transport.A2aTransport;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * A2A Protocol client for communicating with the payment agent.
 *
 * <p>
 * Sends agent-to-agent requests to the payment agent to query refund eligibility for
 * orders. Transport concerns (AgentCard caching, CompletableFuture handling) are
 * delegated to {@link A2aTransport}.
 * </p>
 */
@Slf4j
@Component
public class A2aPaymentAgentClient {

	private static final Pattern REFUND_ELIGIBLE_LINE = Pattern.compile("refundEligible:(true|false)");

	private final A2aTransport transport;

	private final int timeoutSeconds;

	/**
	 * Constructs a payment agent client with the payment agent's base URL and timeout.
	 * @param paymentAgentBaseUrl the base URL of the payment agent
	 * @param timeoutSeconds maximum seconds to wait for a response
	 */
	public A2aPaymentAgentClient(@Value("${order-agent.payment-agent-url}") String paymentAgentBaseUrl,
			@Value("${a2a.client.timeout-seconds}") int timeoutSeconds) {
		this.transport = new A2aTransport(paymentAgentBaseUrl);
		this.timeoutSeconds = timeoutSeconds;
	}

	/**
	 * Retrieves payment and refund eligibility status for an order.
	 *
	 * <p>
	 * Sends an internal A2A request (ROLE_AGENT) with skill ID {@code "payment_status"}
	 * to the payment agent. Returns a non-refund-eligible response if the agent call
	 * fails or times out.
	 * </p>
	 * @param orderNumber the order number to query
	 * @return a {@link PaymentStatusResponse} with the refund eligibility result
	 */
	public PaymentStatusResponse getPaymentStatus(String orderNumber) {
		Message message = Message.builder()
			.role(Message.Role.ROLE_AGENT)
			.parts(List.of(new TextPart(orderNumber)))
			.metadata(Map.of(A2aMetadataKeys.SKILL_ID, "payment_status"))
			.build();
		return transport.send(message, timeoutSeconds).filter(text -> !text.isBlank()).map(text -> {
			var m = REFUND_ELIGIBLE_LINE.matcher(text.trim());
			if (m.find()) {
				return new PaymentStatusResponse(orderNumber, Boolean.parseBoolean(m.group(1)));
			}
			return new PaymentStatusResponse(orderNumber, false);
		}).orElseGet(() -> {
			log.error("결제 에이전트 호출 실패 (orderNumber={})", orderNumber);
			return new PaymentStatusResponse(orderNumber, false);
		});
	}

	/**
	 * Response record containing payment status information for an order.
	 *
	 * @param orderNumber the order number that was queried
	 * @param refundEligible whether the order is eligible for refund
	 */
	public record PaymentStatusResponse(String orderNumber, boolean refundEligible) {
	}

}
