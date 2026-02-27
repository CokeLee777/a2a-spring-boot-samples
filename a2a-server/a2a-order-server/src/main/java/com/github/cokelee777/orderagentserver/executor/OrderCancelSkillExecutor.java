package com.github.cokelee777.orderagentserver.executor;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import com.github.cokelee777.orderagentserver.client.A2aDeliveryAgentClient;
import com.github.cokelee777.orderagentserver.client.A2aPaymentAgentClient;
import com.github.cokelee777.orderagentserver.client.A2aDeliveryAgentClient.DeliveryStatusResponse;
import com.github.cokelee777.orderagentserver.client.A2aPaymentAgentClient.PaymentStatusResponse;
import com.github.cokelee777.orderagentserver.db.OrderDatabase;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelSkillExecutor implements SkillExecutor {

    private final A2aDeliveryAgentClient deliveryAgentClient;
    private final A2aPaymentAgentClient paymentAgentClient;

    public OrderCancelSkillExecutor(A2aDeliveryAgentClient deliveryAgentClient,
                                    A2aPaymentAgentClient paymentAgentClient) {
        this.deliveryAgentClient = deliveryAgentClient;
        this.paymentAgentClient = paymentAgentClient;
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

        // 배송 에이전트·결제 에이전트를 병렬로 호출 (A2A에 맞는 독립 에이전트 동시 조회)
        CompletableFuture<PaymentStatusResponse> paymentFuture = CompletableFuture.supplyAsync(
                () -> paymentAgentClient.getPaymentStatus(orderNumber));
        CompletableFuture<DeliveryStatusResponse> deliveryFuture = (order.trackingNumber() != null && !order.trackingNumber().isBlank())
                ? CompletableFuture.supplyAsync(() -> deliveryAgentClient.getDeliveryStatus(order.trackingNumber()))
                : CompletableFuture.completedFuture(null);

        try {
            PaymentStatusResponse paymentStatus = paymentFuture.get(12, TimeUnit.SECONDS);
            DeliveryStatusResponse deliveryStatus = deliveryFuture.get(12, TimeUnit.SECONDS);

            if (deliveryStatus != null && ("배송중".equals(deliveryStatus.status()) || "배송완료".equals(deliveryStatus.status()))) {
                return String.format(
                        "[취소 불가]\n주문번호: %s\n상품명: %s\n배송상태: %s (배송 에이전트 연동)\n사유: 배송이 진행 중이거나 완료된 주문은 취소할 수 없습니다. 고객센터에 문의해주세요.",
                        order.orderNumber(), order.productName(), deliveryStatus.status()
                );
            }
            if (!paymentStatus.refundEligible()) {
                return String.format(
                        "[취소 불가]\n주문번호: %s\n상품명: %s (결제 에이전트 연동)\n사유: 해당 주문은 환불 처리 가능 상태가 아닙니다. 고객센터에 문의해주세요.",
                        order.orderNumber(), order.productName()
                );
            }
        } catch (Exception e) {
            return "에이전트 호출 중 오류: " + e.getMessage();
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
