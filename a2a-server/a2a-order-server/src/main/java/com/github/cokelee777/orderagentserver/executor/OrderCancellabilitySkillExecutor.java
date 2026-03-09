package com.github.cokelee777.orderagentserver.executor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.github.cokelee777.orderagentserver.client.A2aDeliveryAgentClient;
import com.github.cokelee777.orderagentserver.client.A2aPaymentAgentClient;
import com.github.cokelee777.orderagentserver.client.A2aDeliveryAgentClient.DeliveryStatusResponse;
import com.github.cokelee777.orderagentserver.client.A2aPaymentAgentClient.PaymentStatusResponse;
import com.github.cokelee777.orderagentserver.db.OrderDatabase;
import io.a2a.spec.Message;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Skill executor for determining order cancellation eligibility.
 * <p>
 * Handles the {@code order_cancellability_check} skill. Only accessible to external user
 * calls (ROLE_USER). Implements a parallel calling mechanism to query both the delivery
 * and payment agents concurrently.
 * </p>
 * <p>
 * An order is cancellable only if:
 * </p>
 * <ul>
 * <li>The delivery status is neither "배송중" (in transit) nor "배송완료" (delivered)</li>
 * <li>The payment status indicates the order is refund-eligible</li>
 * </ul>
 */
@Slf4j
@Component
public class OrderCancellabilitySkillExecutor implements SkillExecutor {

	private final A2aDeliveryAgentClient deliveryAgentClient;

	private final A2aPaymentAgentClient paymentAgentClient;

	@Value("${a2a.client.timeout-seconds}")
	private int timeoutSeconds;

	/**
	 * Constructs an OrderCancellabilitySkillExecutor with agent clients.
	 * @param deliveryAgentClient the client for calling the delivery agent
	 * @param paymentAgentClient the client for calling the payment agent
	 */
	public OrderCancellabilitySkillExecutor(A2aDeliveryAgentClient deliveryAgentClient,
			A2aPaymentAgentClient paymentAgentClient) {
		this.deliveryAgentClient = deliveryAgentClient;
		this.paymentAgentClient = paymentAgentClient;
	}

	/**
	 * Returns the skill ID handled by this executor.
	 * @return {@code "order_cancellability_check"}
	 */
	@Override
	public String skillId() {
		return "order_cancellability_check";
	}

	/**
	 * Returns the required caller role for this skill.
	 * @return {@link Message.Role#ROLE_USER} — external calls only
	 */
	@Override
	public Message.Role requiredRole() {
		return Message.Role.ROLE_USER;
	}

	/**
	 * Executes the cancellation eligibility check using parallel agent calls.
	 * <p>
	 * This method:
	 * </p>
	 * <ol>
	 * <li>Extracts the order number from the message</li>
	 * <li>Looks up the order in the database</li>
	 * <li>Initiates two parallel calls to the payment and delivery agents</li>
	 * <li>Waits for both calls to complete within the configured timeout</li>
	 * <li>Combines the responses to determine overall cancellation eligibility</li>
	 * </ol>
	 * @param userMessage the message text containing the order number (ORD-xxx)
	 * @return a formatted report detailing the cancellation eligibility status and
	 * reasons
	 */
	@Override
	public String execute(String userMessage) {
		String orderNumber = extractOrderNumber(userMessage);
		var orderOpt = OrderDatabase.findByOrderNumber(orderNumber);
		if (orderOpt.isEmpty()) {
			return "[조회 결과] 주문번호 '" + orderNumber + "'에 해당하는 주문을 찾을 수 없습니다.";
		}

		var order = orderOpt.get();

		CompletableFuture<PaymentStatusResponse> paymentFuture = CompletableFuture
			.supplyAsync(() -> paymentAgentClient.getPaymentStatus(orderNumber));
		CompletableFuture<DeliveryStatusResponse> deliveryFuture = (order.trackingNumber() != null
				&& !order.trackingNumber().isBlank())
						? CompletableFuture
							.supplyAsync(() -> deliveryAgentClient.getDeliveryStatus(order.trackingNumber()))
						: CompletableFuture.completedFuture(null);

		try {
			PaymentStatusResponse paymentStatus = paymentFuture.get(timeoutSeconds, TimeUnit.SECONDS);
			DeliveryStatusResponse deliveryStatus = deliveryFuture.get(timeoutSeconds, TimeUnit.SECONDS);

			StringBuilder result = new StringBuilder();
			result.append(String.format("[취소 가능 여부 조회 결과]\n주문번호: %s\n상품명: %s\n현재상태: %s\n", order.orderNumber(),
					order.productName(), order.status()));

			boolean cancellable = true;
			StringBuilder reasons = new StringBuilder();

			if (deliveryStatus != null
					&& ("배송중".equals(deliveryStatus.status()) || "배송완료".equals(deliveryStatus.status()))) {
				cancellable = false;
				reasons.append(String.format("- 배송 상태: %s (배송 에이전트 확인 결과, 배송 중이거나 완료된 상품은 취소가 불가합니다.)\n",
						deliveryStatus.status()));
			}

			if (!paymentStatus.refundEligible()) {
				cancellable = false;
				reasons.append("- 결제 상태: 환불 불가 (결제 에이전트 확인 결과, 현재 상태에서는 환불이 불가합니다.)\n");
			}

			if (cancellable) {
				result.append("\n결과: [취소 가능]\n");
				result.append("안내: 현재 해당 주문은 취소가 가능한 상태입니다. 취소를 원하시면 취소 요청을 진행해주세요.");
			}
			else {
				result.append("\n결과: [취소 불가]\n");
				result.append("사유:\n").append(reasons);
				result.append("안내: 배송이 시작되었거나 결제 정책상 취소가 어려울 수 있습니다. 자세한 사항은 고객센터로 문의 바랍니다.");
			}

			return result.toString();
		}
		catch (Exception e) {
			log.error("에이전트 병렬 호출 중 오류 (orderNumber={}): {}", orderNumber, e.getMessage(), e);
			return "에이전트 호출 중 오류: " + e.getMessage();
		}
	}

	/**
	 * Extracts the order number from a message.
	 * @param text the message text
	 * @return the first word starting with "ORD-", or the trimmed text
	 */
	private String extractOrderNumber(String text) {
		for (String word : text.split("\\s+")) {
			if (word.startsWith("ORD-")) {
				return word;
			}
		}
		return text.trim();
	}

}
