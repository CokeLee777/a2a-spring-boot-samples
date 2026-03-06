package com.github.cokelee777.orderagentserver.db;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory database for order information.
 * <p>
 * This class maintains a static collection of order records indexed by order number.
 * </p>
 */
public class OrderDatabase {

	private static final Map<String, OrderInfo> ORDERS = new ConcurrentHashMap<>(Map.of("ORD-1001",
			new OrderInfo("ORD-1001", "결제완료", "무선 키보드", 59000, "2026-02-25 08:00", "TRACK-1001", "user1"), "ORD-2002",
			new OrderInfo("ORD-2002", "배송완료", "USB-C 충전기", 15000, "2026-02-22 10:30", "TRACK-2002", "user1"),
			"ORD-3003",
			new OrderInfo("ORD-3003", "상품준비중", "모니터 거치대", 32000, "2026-02-25 07:00", "TRACK-3003", "user2")));

	/**
	 * Finds an order by its order number.
	 * @param orderNumber the order number to search for
	 * @return an Optional containing the order info, or empty if not found
	 */
	public static Optional<OrderInfo> findByOrderNumber(String orderNumber) {
		return Optional.ofNullable(ORDERS.get(orderNumber));
	}

	/**
	 * Finds an order by its tracking number.
	 * @param trackingNumber the tracking number to search for
	 * @return an Optional containing the order info, or empty if not found
	 */
	public static Optional<OrderInfo> findByTrackingNumber(String trackingNumber) {
		return ORDERS.values().stream().filter(o -> trackingNumber.equals(o.trackingNumber())).findFirst();
	}

	/**
	 * Finds all orders belonging to a specific member.
	 * @param memberId the member ID to search for
	 * @return a list of orders for the given member, or an empty list if none found
	 */
	public static List<OrderInfo> findAllByMemberId(String memberId) {
		return ORDERS.values().stream().filter(o -> memberId != null && memberId.equals(o.memberId())).toList();
	}

	/**
	 * Represents an order information record.
	 *
	 * @param orderNumber the unique order number
	 * @param status the current order status (e.g., "결제완료", "배송완료", "상품준비중")
	 * @param productName the name of the ordered product
	 * @param price the price of the product
	 * @param orderDate the timestamp when the order was placed
	 * @param trackingNumber the associated tracking number for delivery, or null
	 * @param memberId the ID of the member who placed the order
	 */
	public record OrderInfo(String orderNumber, String status, String productName, int price, String orderDate,
			String trackingNumber, String memberId) {
	}

}
