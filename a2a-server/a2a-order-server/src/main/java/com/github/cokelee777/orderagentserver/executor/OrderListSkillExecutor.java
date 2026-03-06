package com.github.cokelee777.orderagentserver.executor;

import com.github.cokelee777.orderagentserver.db.OrderDatabase;
import com.github.cokelee777.orderagentserver.db.OrderDatabase.OrderInfo;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

/**
 * 회원 ID로 주문 내역을 조회합니다. 클라이언트에서 "MEMBER-{memberId} 주문내역 조회" 형식으로 호출합니다.
 */
@Component
public class OrderListSkillExecutor implements SkillExecutor {

	private static final Pattern MEMBER_PREFIX = Pattern.compile("^MEMBER-(\\S+).*");

	@Override
	public boolean canHandle(String message, boolean isInternalCall) {
		if (isInternalCall || message == null || message.isBlank())
			return false;
		return MEMBER_PREFIX.matcher(message.trim()).matches();
	}

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

	private String extractMemberId(String text) {
		if (!text.startsWith("MEMBER-"))
			return null;
		String rest = text.substring(7).trim();
		int space = rest.indexOf(' ');
		return space > 0 ? rest.substring(0, space) : rest;
	}

}
