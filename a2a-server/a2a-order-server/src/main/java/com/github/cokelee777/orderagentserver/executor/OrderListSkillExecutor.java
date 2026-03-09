package com.github.cokelee777.orderagentserver.executor;

import com.github.cokelee777.a2a.server.common.executor.SkillExecutor;
import com.github.cokelee777.orderagentserver.OrderAgentSkillIds;
import com.github.cokelee777.orderagentserver.db.OrderDatabase;
import com.github.cokelee777.orderagentserver.db.OrderDatabase.OrderInfo;
import io.a2a.spec.Message;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Skill executor for querying and listing orders by member ID.
 * <p>
 * Handles the {@code order_list} skill. Only accessible to external user calls
 * (ROLE_USER).
 * </p>
 */
@Component
public class OrderListSkillExecutor implements SkillExecutor {

	/**
	 * Returns the skill ID handled by this executor.
	 * @return {@link OrderAgentSkillIds#ORDER_LIST}
	 */
	@Override
	public String skillId() {
		return OrderAgentSkillIds.ORDER_LIST;
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
	 * Executes the order list query for a given member.
	 * <p>
	 * Extracts the member ID from the message, queries the database, and returns a
	 * formatted list of all orders for that member.
	 * </p>
	 * @param message the message text containing "MEMBER-{memberId}"
	 * @return a formatted string with all orders for the member, or an error message
	 */
	@Override
	public String execute(String message) {
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
	 * @return the member ID, or null if the message does not match
	 */
	private String extractMemberId(String text) {
		if (!text.startsWith("MEMBER-"))
			return null;
		String rest = text.substring(7).trim();
		int space = rest.indexOf(' ');
		return space > 0 ? rest.substring(0, space) : rest;
	}

}
