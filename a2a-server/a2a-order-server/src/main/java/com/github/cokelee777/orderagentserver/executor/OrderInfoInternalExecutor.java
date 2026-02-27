package com.github.cokelee777.orderagentserver.executor;

import com.github.cokelee777.orderagentserver.db.OrderDatabase;
import org.springframework.stereotype.Component;

/**
 * A2A 에이전트 간 통신용: 배송 에이전트가 운송장번호로 주문 정보를 요청할 때 처리합니다.
 * 메시지 형식: 운송장번호 (TRACK-xxx) - Message.Role.ROLE_AGENT로 전달
 * 응답 형식: orderNumber:...\norderDate:...\nstatus:... (파싱 가능한 한 줄씩)
 */
@Component
public class OrderInfoInternalExecutor implements SkillExecutor {

    @Override
    public boolean canHandle(String message, boolean isInternalCall) {
        return isInternalCall && message != null && message.startsWith("TRACK-");
    }

    @Override
    public String execute(String message, boolean isInternalCall) {
        String trackingNumber = message.trim();
        return OrderDatabase.findByTrackingNumber(trackingNumber)
                .map(order -> "orderNumber:" + order.orderNumber() + "\n"
                        + "productName:" + order.productName() + "\n"
                        + "orderDate:" + order.orderDate() + "\n"
                        + "status:" + order.status())
                .orElse("orderNumber:NOT_FOUND\norderDate:\nstatus:NOT_FOUND");
    }
}
