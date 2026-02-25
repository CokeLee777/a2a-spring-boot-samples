package com.github.cokelee777.orderagentserver.executor;

import com.github.cokelee777.orderagentserver.db.OrderDatabase;
import org.springframework.stereotype.Component;

/**
 * A2A 에이전트 간 통신용: 배송 에이전트가 운송장번호로 주문 정보를 요청할 때 처리합니다.
 * 메시지 형식: "[A2A-INTERNAL] order-info TRACK-xxx"
 * 응답 형식: orderNumber:...\norderDate:...\nstatus:... (파싱 가능한 한 줄씩)
 */
@Component
public class OrderInfoInternalExecutor implements SkillExecutor {

    private static final String A2A_INTERNAL_PREFIX = "[A2A-INTERNAL] order-info ";

    @Override
    public boolean canHandle(String userMessage) {
        return userMessage != null && userMessage.startsWith(A2A_INTERNAL_PREFIX);
    }

    @Override
    public String execute(String userMessage) {
        String trackingNumber = userMessage.substring(A2A_INTERNAL_PREFIX.length()).trim();
        return OrderDatabase.findByTrackingNumber(trackingNumber)
                .map(order -> "orderNumber:" + order.orderNumber() + "\n"
                        + "productName:" + order.productName() + "\n"
                        + "orderDate:" + order.orderDate() + "\n"
                        + "status:" + order.status())
                .orElse("orderNumber:NOT_FOUND\norderDate:\nstatus:NOT_FOUND");
    }
}
