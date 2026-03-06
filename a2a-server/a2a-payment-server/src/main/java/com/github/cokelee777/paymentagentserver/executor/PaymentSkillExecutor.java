package com.github.cokelee777.paymentagentserver.executor;

import com.github.cokelee777.paymentagentserver.db.PaymentDatabase;
import org.springframework.stereotype.Component;

/**
 * Skill executor for retrieving payment and refund eligibility information.
 * <p>
 * This executor handles internal agent-to-agent requests to look up payment status and
 * refund eligibility using an order number. It is only accessible to internal A2A calls,
 * not external user requests.
 * </p>
 */
@Component
public class PaymentSkillExecutor implements SkillExecutor {

	/**
	 * Determines whether this executor can handle internal payment info requests.
	 * <p>
	 * This executor handles internal requests starting with "ORD-" prefix.
	 * </p>
	 * @param message the user message text
	 * @param isInternalCall whether this is an internal agent-to-agent call
	 * @return true if the message starts with "ORD-" and is an internal call
	 */
	@Override
	public boolean canHandle(String message, boolean isInternalCall) {
		return isInternalCall && message != null && message.startsWith("ORD-");
	}

	/**
	 * Executes the payment information lookup by order number.
	 * <p>
	 * For internal A2A calls, queries the payment database and returns refund eligibility
	 * in a key-value format for internal A2A consumption.
	 * </p>
	 * @param message the order number (ORD-xxx)
	 * @param isInternalCall whether this is an internal agent-to-agent call
	 * @return a formatted string with refund eligibility status for internal A2A calls,
	 * or an error message for external calls
	 */
	@Override
	public String execute(String message, boolean isInternalCall) {
		if (isInternalCall) {
			return PaymentDatabase.getRefundEligibleLine(message.trim());
		}
		return "결제 상태 조회는 주문 에이전트에서 A2A 내부 요청으로만 호출됩니다.";
	}

}
