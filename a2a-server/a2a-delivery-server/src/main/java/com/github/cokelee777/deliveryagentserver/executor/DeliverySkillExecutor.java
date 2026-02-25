package com.github.cokelee777.deliveryagentserver.executor;

import com.github.cokelee777.deliveryagentserver.client.A2aOrderAgentClient;
import com.github.cokelee777.deliveryagentserver.db.DeliveryDatabase;
import org.springframework.stereotype.Component;

@Component
public class DeliverySkillExecutor implements SkillExecutor {

    /** A2A 내부 요청: 배송 상태만 반환 (에이전트 간 통신용) */
    private static final String A2A_INTERNAL_PREFIX = "[A2A-INTERNAL] delivery-status ";

    private final A2aOrderAgentClient orderAgentClient;

    public DeliverySkillExecutor(A2aOrderAgentClient orderAgentClient) {
        this.orderAgentClient = orderAgentClient;
    }

    @Override
    public boolean canHandle(String userMessage) {
        if (userMessage != null && userMessage.startsWith(A2A_INTERNAL_PREFIX)) {
            return true;
        }
        for (String word : userMessage.split("\\s+")) {
            if (word.startsWith("TRACK-")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String execute(String userMessage) {
        if (userMessage != null && userMessage.startsWith(A2A_INTERNAL_PREFIX)) {
            String trackingNumber = userMessage.substring(A2A_INTERNAL_PREFIX.length()).trim();
            return DeliveryDatabase.findById(trackingNumber)
                    .map(info -> "status:" + info.status())
                    .orElse("status:NOT_FOUND");
        }

        String trackingNumber = extractTrackingNumber(userMessage);
        String baseResult = DeliveryDatabase.lookup(trackingNumber);

        var orderInfo = orderAgentClient.getOrderInfo(trackingNumber);
        if (orderInfo != null) {
            return baseResult + String.format(
                    "\n\n[주문 에이전트 연동 정보]\n주문번호: %s\n주문일시: %s\n주문상태: %s",
                    orderInfo.orderNumber(), orderInfo.orderDate(), orderInfo.status()
            );
        }

        return baseResult;
    }

    private String extractTrackingNumber(String text) {
        for (String word : text.split("\\s+")) {
            if (word.startsWith("TRACK-")) {
                return word;
            }
        }
        return text.trim();
    }
}
