package com.github.cokelee777.a2aserver.db;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class OrderDatabase {

    private static final Map<String, OrderInfo> ORDERS = new ConcurrentHashMap<>(Map.of(
            "ORD-1001", new OrderInfo("ORD-1001", "결제완료", "무선 키보드", 59000,
                    "2026-02-25 08:00", "TRACK-1001"),
            "ORD-2002", new OrderInfo("ORD-2002", "배송완료", "USB-C 충전기", 15000,
                    "2026-02-22 10:30", "TRACK-2002"),
            "ORD-3003", new OrderInfo("ORD-3003", "상품준비중", "모니터 거치대", 32000,
                    "2026-02-25 07:00", "TRACK-3003")
    ));

    public static String cancel(String orderNumber) {
        OrderInfo info = ORDERS.get(orderNumber);
        if (info == null) {
            return "[취소 실패] 주문번호 '" + orderNumber + "'에 해당하는 주문을 찾을 수 없습니다.";
        }

        if ("배송중".equals(info.status()) || "배송완료".equals(info.status())) {
            return String.format(
                    "[취소 불가]\n주문번호: %s\n상품명: %s\n현재상태: %s\n사유: %s 상태에서는 주문을 취소할 수 없습니다. 고객센터에 문의해주세요.",
                    info.orderNumber(), info.productName(), info.status(), info.status()
            );
        }

        OrderInfo cancelled = new OrderInfo(
                info.orderNumber(), "취소완료", info.productName(),
                info.price(), info.orderDate(), info.trackingNumber()
        );
        ORDERS.put(orderNumber, cancelled);

        return String.format(
                "[주문 취소 완료]\n주문번호: %s\n상품명: %s\n결제금액: %,d원\n주문일시: %s\n환불 예정: 영업일 기준 3~5일 내 환불됩니다.",
                cancelled.orderNumber(), cancelled.productName(),
                cancelled.price(), cancelled.orderDate()
        );
    }

    record OrderInfo(
            String orderNumber,
            String status,
            String productName,
            int price,
            String orderDate,
            String trackingNumber
    ) {}
}
