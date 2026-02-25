package com.github.cokelee777.orderagentserver.executor;

import com.github.cokelee777.orderagentserver.client.A2aDeliveryAgentClient;
import com.github.cokelee777.orderagentserver.db.OrderDatabase;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelSkillExecutor implements SkillExecutor {

    private final A2aDeliveryAgentClient deliveryAgentClient;

    public OrderCancelSkillExecutor(A2aDeliveryAgentClient deliveryAgentClient) {
        this.deliveryAgentClient = deliveryAgentClient;
    }

    @Override
    public boolean canHandle(String userMessage) {
        for (String word : userMessage.split("\\s+")) {
            if (word.startsWith("ORD-")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String execute(String userMessage) {
        String orderNumber = extractOrderNumber(userMessage);
        var orderOpt = OrderDatabase.findByOrderNumber(orderNumber);
        if (orderOpt.isEmpty()) {
            return OrderDatabase.cancel(orderNumber);
        }

        var order = orderOpt.get();
        if (order.trackingNumber() != null && !order.trackingNumber().isBlank()) {
            var deliveryStatus = deliveryAgentClient.getDeliveryStatus(order.trackingNumber());
            if (deliveryStatus != null && ("배송중".equals(deliveryStatus.status()) || "배송완료".equals(deliveryStatus.status()))) {
                return String.format(
                        "[취소 불가]\n주문번호: %s\n상품명: %s\n배송상태: %s (배송 에이전트 연동)\n사유: 배송이 진행 중이거나 완료된 주문은 취소할 수 없습니다. 고객센터에 문의해주세요.",
                        order.orderNumber(), order.productName(), deliveryStatus.status()
                );
            }
        }

        return OrderDatabase.cancel(orderNumber);
    }

    private String extractOrderNumber(String text) {
        for (String word : text.split("\\s+")) {
            if (word.startsWith("ORD-")) {
                return word;
            }
        }
        return text.trim();
    }
}
