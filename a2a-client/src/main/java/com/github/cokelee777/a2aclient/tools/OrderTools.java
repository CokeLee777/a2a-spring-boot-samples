package com.github.cokelee777.a2aclient.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderTools extends A2aTools {

	public record OrderRequest(String orderNumber) {
	}

	public OrderTools(@Value("${a2a.order-agent-url}") String agentUrl) {
		super(agentUrl);
	}

	@Tool(description = "주문 취소 가능 여부 확인. 주문번호(ORD-xxxx)가 필요합니다.")
	public String checkOrderCancellability(OrderRequest request) {
		return sendRequest(request.orderNumber() + " 취소 가능 여부 확인");
	}

}
