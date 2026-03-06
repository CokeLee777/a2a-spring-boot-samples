package com.github.cokelee777.a2aclient.tools;

import org.springframework.ai.tool.annotation.Tool;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeliveryTools extends A2aTools {

	public record DeliveryRequest(String trackingNumber) {
	}

	public DeliveryTools(@Value("${a2a.delivery-agent-url}") String agentUrl) {
		super(agentUrl);
	}

	@Tool(description = "배송 조회. 운송장번호(TRACK-xxxx)가 필요합니다.")
	public String trackDelivery(DeliveryRequest request) {
		return sendRequest(request.trackingNumber() + " 배송 조회");
	}

}
