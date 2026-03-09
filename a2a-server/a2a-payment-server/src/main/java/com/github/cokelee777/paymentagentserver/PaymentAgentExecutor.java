package com.github.cokelee777.paymentagentserver;

import com.github.cokelee777.a2a.common.metadata.A2aMetadataKeys;
import com.github.cokelee777.a2a.common.util.TextExtractor;
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
 * Routes incoming requests to the appropriate {@link SkillExecutor} by reading the
 * {@code skillId} from the message metadata. The caller's {@link Message.Role} is
 * validated against each executor's {@link SkillExecutor#requiredRole()}.
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
		String text = TextExtractor.extractFromMessage(message);
		try {
			String result = routeToSkill(extractSkillId(message), text, message.role());
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
	 * Routes the request to the matching skill executor by skill ID and role.
	 * @param skillId the skill ID extracted from message metadata
	 * @param text the message text
	 * @param role the caller's A2A role
	 * @return the execution result, or a fallback message if no executor matches
	 */
	private String routeToSkill(String skillId, String text, Message.Role role) {
		if (skillId == null) {
			return "결제 상태 조회는 주문번호(ORD-)를 포함해 주세요.";
		}
		for (SkillExecutor executor : skillExecutors) {
			if (executor.skillId().equals(skillId)) {
				if (!executor.requiredRole().equals(role)) {
					return "접근 권한이 없습니다.";
				}
				return executor.execute(text);
			}
		}
		return "결제 상태 조회는 주문번호(ORD-)를 포함해 주세요.";
	}

	/**
	 * Extracts the skill ID from message metadata.
	 * @param message the incoming A2A message
	 * @return the skill ID string, or {@code null} if not present
	 */
	private String extractSkillId(Message message) {
		if (message.metadata() == null) {
			return null;
		}
		Object val = message.metadata().get(A2aMetadataKeys.SKILL_ID);
		return val instanceof String s ? s : null;
	}

}
