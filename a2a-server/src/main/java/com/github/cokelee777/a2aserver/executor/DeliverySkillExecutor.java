package com.github.cokelee777.a2aserver.executor;

import com.github.cokelee777.a2aserver.db.DeliveryDatabase;
import org.springframework.stereotype.Component;

@Component
public class DeliverySkillExecutor implements SkillExecutor {

    @Override
    public boolean canHandle(String userMessage) {
        for (String word : userMessage.split("\\s+")) {
            if (word.startsWith("TRACK-")) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String execute(String userMessage) {
        String trackingNumber = extractTrackingNumber(userMessage);
        return DeliveryDatabase.lookup(trackingNumber);
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
