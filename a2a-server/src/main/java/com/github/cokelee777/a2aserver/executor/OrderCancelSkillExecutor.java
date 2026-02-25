package com.github.cokelee777.a2aserver.executor;

import com.github.cokelee777.a2aserver.db.OrderDatabase;
import org.springframework.stereotype.Component;

@Component
public class OrderCancelSkillExecutor implements SkillExecutor {

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
