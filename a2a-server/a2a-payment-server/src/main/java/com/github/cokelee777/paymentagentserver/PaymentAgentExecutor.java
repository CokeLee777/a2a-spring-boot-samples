package com.github.cokelee777.paymentagentserver;

import com.github.cokelee777.a2a.server.utils.MessageUtil;
import com.github.cokelee777.paymentagentserver.executor.SkillExecutor;
import io.a2a.server.agentexecution.AgentExecutor;
import io.a2a.server.agentexecution.RequestContext;
import io.a2a.server.tasks.AgentEmitter;
import io.a2a.spec.A2AError;
import io.a2a.spec.Message;
import io.a2a.spec.TextPart;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;

/**
 * A2A AgentExecutor implementation for the payment agent.
 *
 * <p>
 * Determines whether the request is an internal agent call by checking the message role,
 * then routes to the appropriate {@link SkillExecutor}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PaymentAgentExecutor implements AgentExecutor {

	private final List<SkillExecutor> skillExecutors;

	/**
	 * Executes the payment agent logic for the given request context.
	 * @param context the request context containing message and task information
	 * @param emitter the agent emitter for publishing task state updates and artifacts
	 * @throws A2AError if an A2A protocol error occurs
	 */
	@Override
	public void execute(RequestContext context, AgentEmitter emitter) throws A2AError {
		emitter.startWork();

		Message message = Objects.requireNonNull(context.getMessage(), "message can not be null");
		boolean isInternal = Message.Role.ROLE_AGENT.equals(message.role());
		String text = MessageUtil.extractTextFromMessage(context.getMessage());
		try {
			String result = routeToSkill(text, isInternal);
			emitter.addArtifact(List.of(new TextPart(result)));
			emitter.complete();
		}
		catch (Exception e) {
			log.error("Payment agent execution error: {}", e.getMessage(), e);
			emitter.fail();
		}
	}

	/**
	 * Cancels the current payment agent task.
	 * @param context the request context
	 * @param emitter the agent emitter to signal cancellation
	 * @throws A2AError if an A2A protocol error occurs
	 */
	@Override
	public void cancel(RequestContext context, AgentEmitter emitter) throws A2AError {
		emitter.cancel();
	}

	/**
	 * Routes the message text to the appropriate skill executor.
	 * @param text the message text extracted from the request
	 * @param isInternal true if this is an internal agent-to-agent call
	 * @return the result from the matched executor, or a fallback message
	 */
	private String routeToSkill(String text, boolean isInternal) {
		for (SkillExecutor executor : skillExecutors) {
			if (executor.canHandle(text, isInternal)) {
				return executor.execute(text, isInternal);
			}
		}
		return "결제 상태 조회는 주문번호(ORD-)를 포함해 주세요.";
	}

}
