package com.github.cokelee777.a2aserver.executor;

public interface SkillExecutor {

    boolean canHandle(String userMessage);

    String execute(String userMessage);
}
