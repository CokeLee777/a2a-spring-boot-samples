package com.github.cokelee777.orderagentserver.db;

import java.util.Map;
import java.util.Optional;
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

    public static Optional<OrderInfo> findByOrderNumber(String orderNumber) {
        return Optional.ofNullable(ORDERS.get(orderNumber));
    }

    public static Optional<OrderInfo> findByTrackingNumber(String trackingNumber) {
        return ORDERS.values().stream()
                .filter(o -> trackingNumber.equals(o.trackingNumber()))
                .findFirst();
    }

    public record OrderInfo(
            String orderNumber,
            String status,
            String productName,
            int price,
            String orderDate,
            String trackingNumber
    ) {}
}
