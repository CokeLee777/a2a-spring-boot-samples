package com.github.cokelee777.paymentagentserver.executor;

public interface SkillExecutor {

    boolean canHandle(String userMessage);

    String execute(String userMessage);
}
