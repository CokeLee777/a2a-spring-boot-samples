package com.github.cokelee777.a2aclient.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Spring AI tools for order-related operations.
 * <p>
 * This component extends {@link A2aTool} to provide LLM-callable tools for order
 * management. These tools communicate with the order agent via the A2A Protocol to query
 * order information and check cancellation eligibility.
 * </p>
 */
@Component
public class OrderTool extends A2aTool {

	/**
	 * Request object for retrieving an order list.
	 *
	 * @param memberId the unique identifier of the member
	 */
	public record OrderListRequest(String memberId) {
	}

	/**
	 * Request object for checking order cancellation eligibility.
	 *
	 * @param orderNumber the order number in format "ORD-xxxx"
	 */
	public record OrderCancellabilityRequest(String orderNumber) {
	}

	/**
	 * Constructs an OrderTools instance with the configured order agent URL.
	 * @param agentUrl the base URL of the order agent
	 */
	public OrderTool(@Value("${a2a.order-agent-url}") String agentUrl) {
		super(agentUrl);
	}

	/**
	 * Retrieves the order list for a specific member.
	 * <p>
	 * This tool queries the order agent to fetch all orders associated with the given
	 * member ID. It is typically invoked when the user requests to view their order
	 * history.
	 * </p>
	 * @param request the request containing the member ID
	 * @return a formatted response from the order agent describing the member's orders
	 */
	@Tool(description = "해당 회원의 주문 내역(목록)을 조회합니다. 현재 사용자 ID(memberId)가 필요합니다. '내 주문 보여줘', '주문 목록 조회' 등일 때 사용하세요.")
	public String getOrderList(OrderListRequest request) {
		return sendRequest("MEMBER-" + request.memberId() + " 주문내역 조회");
	}

	/**
	 * Checks whether a specific order can be cancelled.
	 * <p>
	 * This tool queries the order agent to determine the cancellation eligibility of the
	 * specified order. It is typically invoked when the user asks about cancelling a
	 * particular order.
	 * </p>
	 * @param request the request containing the order number (e.g., "ORD-1001")
	 * @return a formatted response from the order agent indicating cancellation
	 * eligibility
	 */
	@Tool(description = "주문 취소 가능 여부 확인. 주문번호(ORD-xxxx)가 필요합니다. 이전 대화에서 나온 주문번호를 사용할 수 있습니다.")
	public String checkOrderCancellability(OrderCancellabilityRequest request) {
		return sendRequest(request.orderNumber() + " 취소 가능 여부 확인");
	}

}
