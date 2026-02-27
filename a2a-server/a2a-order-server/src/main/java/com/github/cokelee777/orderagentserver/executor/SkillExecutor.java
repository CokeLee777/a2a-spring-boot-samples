package com.github.cokelee777.orderagentserver.executor;

public interface SkillExecutor {

    boolean canHandle(String message, boolean isInternalCall);

    String execute(String message, boolean isInternalCall);
}
