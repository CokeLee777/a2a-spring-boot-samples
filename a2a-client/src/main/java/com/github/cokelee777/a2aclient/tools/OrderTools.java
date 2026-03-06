package com.github.cokelee777.a2aclient.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderTools extends A2aTools {

	public record OrderListRequest(String memberId) {
	}

	public record OrderCancellabilityRequest(String orderNumber) {
	}

	public OrderTools(@Value("${a2a.order-agent-url}") String agentUrl) {
		super(agentUrl);
	}

	@Tool(description = "해당 회원의 주문 내역(목록)을 조회합니다. 현재 사용자 ID(memberId)가 필요합니다. '내 주문 보여줘', '주문 목록 조회' 등일 때 사용하세요.")
	public String getOrderList(OrderListRequest request) {
		return sendRequest("MEMBER-" + request.memberId() + " 주문내역 조회");
	}

	@Tool(description = "주문 취소 가능 여부 확인. 주문번호(ORD-xxxx)가 필요합니다. 이전 대화에서 나온 주문번호를 사용할 수 있습니다.")
	public String checkOrderCancellability(OrderCancellabilityRequest request) {
		return sendRequest(request.orderNumber() + " 취소 가능 여부 확인");
	}

}
