package com.github.cokelee777.paymentagentserver.db;

import java.util.Map;
import java.util.Optional;

/**
 * 주문번호별 결제·환불 상태 (데모용 인메모리).
 * 주문 에이전트가 "[A2A-INTERNAL] payment-status ORD-xxx" 로 조회 시 환불 가능 여부를 반환합니다.
 */
public class PaymentDatabase {

    private static final Map<String, PaymentInfo> PAYMENTS = Map.of(
            "ORD-1001", new PaymentInfo("ORD-1001", "결제완료", true),
            "ORD-2002", new PaymentInfo("ORD-2002", "결제완료", true),
            "ORD-3003", new PaymentInfo("ORD-3003", "환불처리중", false)
    );

    public static Optional<PaymentInfo> findByOrderNumber(String orderNumber) {
        return Optional.ofNullable(PAYMENTS.get(orderNumber));
    }

    /** A2A 내부 응답용: refundEligible 한 줄 (주문 에이전트가 파싱) */
    public static String getRefundEligibleLine(String orderNumber) {
        return findByOrderNumber(orderNumber)
                .map(p -> "refundEligible:" + p.refundEligible())
                .orElse("refundEligible:false");
    }

    public record PaymentInfo(String orderNumber, String status, boolean refundEligible) {}
}
