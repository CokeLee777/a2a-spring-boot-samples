package com.github.cokelee777.paymentagentserver.db;

import java.util.Map;
import java.util.Optional;

/**
 * In-memory database for payment information.
 * <p>
 * This class maintains a static collection of payment records indexed by order number.
 * </p>
 */
public class PaymentDatabase {

	private static final Map<String, PaymentInfo> PAYMENTS = Map.of("ORD-1001",
			new PaymentInfo("ORD-1001", "결제완료", true), "ORD-2002", new PaymentInfo("ORD-2002", "결제완료", true),
			"ORD-3003", new PaymentInfo("ORD-3003", "환불처리중", false));

	/**
	 * Finds payment information by order number.
	 * @param orderNumber the order number to search for
	 * @return an Optional containing the payment info, or empty if not found
	 */
	public static Optional<PaymentInfo> findByOrderNumber(String orderNumber) {
		return Optional.ofNullable(PAYMENTS.get(orderNumber));
	}

	/**
	 * Retrieves refund eligibility status in A2A response format.
	 * <p>
	 * Returns a single line formatted as "refundEligible:true|false" for internal A2A
	 * consumption by the order agent.
	 * </p>
	 * @param orderNumber the order number to query
	 * @return a formatted string with refund eligibility status
	 */
	public static String getRefundEligibleLine(String orderNumber) {
		return findByOrderNumber(orderNumber).map(p -> "refundEligible:" + p.refundEligible())
			.orElse("refundEligible:false");
	}

	/**
	 * Represents payment information for an order.
	 *
	 * @param orderNumber the unique order number
	 * @param status the current payment status (e.g., "결제완료", "환불처리중")
	 * @param refundEligible whether the order is eligible for refund
	 */
	public record PaymentInfo(String orderNumber, String status, boolean refundEligible) {
	}

}
