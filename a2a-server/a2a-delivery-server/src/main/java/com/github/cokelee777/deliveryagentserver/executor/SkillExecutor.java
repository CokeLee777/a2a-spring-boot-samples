package com.github.cokelee777.deliveryagentserver.executor;

public interface SkillExecutor {

    boolean canHandle(String message, boolean isInternalCall);

    String execute(String message, boolean isInternalCall);
}
