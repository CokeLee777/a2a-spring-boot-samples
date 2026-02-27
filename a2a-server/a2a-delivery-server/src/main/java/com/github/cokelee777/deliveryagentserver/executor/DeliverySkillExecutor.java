package com.github.cokelee777.deliveryagentserver.executor;

import com.github.cokelee777.deliveryagentserver.client.A2aOrderAgentClient;
import com.github.cokelee777.deliveryagentserver.db.DeliveryDatabase;
import org.springframework.stereotype.Component;

@Component
public class DeliverySkillExecutor implements SkillExecutor {

    private final A2aOrderAgentClient orderAgentClient;

    public DeliverySkillExecutor(A2aOrderAgentClient orderAgentClient) {
        this.orderAgentClient = orderAgentClient;
    }

    @Override
    public boolean canHandle(String message, boolean isInternalCall) {
        if (message == null) return false;
        for (String word : message.split("\\s+")) {
            if (word.startsWith("TRACK-")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String execute(String message, boolean isInternalCall) {
        if (isInternalCall) {
            return DeliveryDatabase.findById(message.trim())
                    .map(info -> "status:" + info.status())
                    .orElse("status:NOT_FOUND");
        }

        String trackingNumber = extractTrackingNumber(message);
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
