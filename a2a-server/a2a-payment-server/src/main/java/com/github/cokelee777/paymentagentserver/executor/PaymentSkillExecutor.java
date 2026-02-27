package com.github.cokelee777.paymentagentserver.executor;

import com.github.cokelee777.paymentagentserver.db.PaymentDatabase;
import org.springframework.stereotype.Component;

/**
 * A2A 내부 요청만 처리: 주문 에이전트가 "payment-status ORD-xxx" 로 환불 가능 여부를 조회합니다.
 */
@Component
public class PaymentSkillExecutor implements SkillExecutor {

    private static final String A2A_INTERNAL_PREFIX = "[A2A-INTERNAL] payment-status ";

    @Override
    public boolean canHandle(String userMessage) {
        if (userMessage != null && userMessage.startsWith(A2A_INTERNAL_PREFIX)) {
            return true;
        }
        for (String word : userMessage.split("\\s+")) {
            if (word.startsWith("ORD-")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String execute(String userMessage) {
        if (userMessage != null && userMessage.startsWith(A2A_INTERNAL_PREFIX)) {
            String orderNumber = userMessage.substring(A2A_INTERNAL_PREFIX.length()).trim();
            return PaymentDatabase.getRefundEligibleLine(orderNumber);
        }
        return "결제 상태 조회는 주문 에이전트에서 A2A 내부 요청으로만 호출됩니다.";
    }
}
