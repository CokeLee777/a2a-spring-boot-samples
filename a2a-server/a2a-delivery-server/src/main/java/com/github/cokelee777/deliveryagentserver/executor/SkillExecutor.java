package com.github.cokelee777.deliveryagentserver.executor;

public interface SkillExecutor {

    boolean canHandle(String userMessage);

    String execute(String userMessage);
}
