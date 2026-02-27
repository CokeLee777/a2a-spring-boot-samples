package com.github.cokelee777.paymentagentserver.executor;

import com.github.cokelee777.paymentagentserver.db.PaymentDatabase;
import org.springframework.stereotype.Component;

/**
 * A2A 내부 요청만 처리: 주문 에이전트가 ORD-xxx 로 환불 가능 여부를 조회합니다.
 * Message.Role.ROLE_AGENT로 전달된 요청만 처리합니다.
 */
@Component
public class PaymentSkillExecutor implements SkillExecutor {

    @Override
    public boolean canHandle(String message, boolean isInternalCall) {
        return isInternalCall && message != null && message.startsWith("ORD-");
    }

    @Override
    public String execute(String message, boolean isInternalCall) {
        if (isInternalCall) {
            return PaymentDatabase.getRefundEligibleLine(message.trim());
        }
        return "결제 상태 조회는 주문 에이전트에서 A2A 내부 요청으로만 호출됩니다.";
    }
}
