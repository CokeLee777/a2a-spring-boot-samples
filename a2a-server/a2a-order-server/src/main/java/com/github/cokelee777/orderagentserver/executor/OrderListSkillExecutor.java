package com.github.cokelee777.orderagentserver.executor;

import com.github.cokelee777.orderagentserver.db.OrderDatabase;
import com.github.cokelee777.orderagentserver.db.OrderDatabase.OrderInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * Skill executor for querying and listing orders by member ID.
 * <p>
 * This executor handles external user requests with the prefix "MEMBER-" followed by a
 * member ID, and returns a formatted list of all orders associated with that member.
 * </p>
 */
@Component
public class OrderListSkillExecutor implements SkillExecutor {

	private static final Pattern MEMBER_PREFIX = Pattern.compile("^MEMBER-(\\S+).*");

	/**
	 * Determines whether this executor can handle order list queries.
	 * <p>
	 * This executor handles external requests starting with "MEMBER-" prefix.
	 * </p>
	 * @param message the user message text
	 * @param isInternalCall whether this is an internal agent-to-agent call
	 * @return true if the message starts with "MEMBER-" and is not an internal call
	 */
	@Override
	public boolean canHandle(String message, boolean isInternalCall) {
		if (isInternalCall || message == null || message.isBlank())
			return false;
		return MEMBER_PREFIX.matcher(message.trim()).matches();
	}

	/**
	 * Executes the order list query for a given member.
	 * <p>
	 * Extracts the member ID from the message, queries the database, and returns a
	 * formatted list of all orders for that member.
	 * </p>
	 * @param message the user message text containing "MEMBER-{memberId}"
	 * @param isInternalCall whether this is an internal agent-to-agent call
	 * @return a formatted string with all orders for the member, or an error message
	 */
	@Override
	public String execute(String message, boolean isInternalCall) {
		String memberId = extractMemberId(message.trim());
		if (memberId == null || memberId.isBlank()) {
			return "[조회 결과] 회원 ID를 확인할 수 없습니다.";
		}
		List<OrderInfo> orders = OrderDatabase.findAllByMemberId(memberId);
		if (orders.isEmpty()) {
			return String.format("[주문 내역 조회 결과] 회원 ID '%s'에 해당하는 주문이 없습니다.", memberId);
		}
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("[주문 내역 조회 결과] 회원 ID: %s, 총 %d건\n\n", memberId, orders.size()));
		for (OrderInfo o : orders) {
			sb.append(String.format("- 주문번호: %s | 상품명: %s | 상태: %s | 주문일: %s | 운송장: %s\n", o.orderNumber(),
					o.productName(), o.status(), o.orderDate(), o.trackingNumber() != null ? o.trackingNumber() : "-"));
		}
		sb.append("\n취소 가능 여부를 알고 싶으면 주문번호(예: ORD-1001)를 말씀해 주세요.");
		return sb.toString();
	}

	/**
	 * Extracts the member ID from a message starting with "MEMBER-".
	 * @param text the message text
	 * @return the member ID, or null if the message does not start with "MEMBER-"
	 */
	private String extractMemberId(String text) {
		if (!text.startsWith("MEMBER-"))
			return null;
		String rest = text.substring(7).trim();
		int space = rest.indexOf(' ');
		return space > 0 ? rest.substring(0, space) : rest;
	}

}
