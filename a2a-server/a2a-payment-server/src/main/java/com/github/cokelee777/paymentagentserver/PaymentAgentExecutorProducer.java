package com.github.cokelee777.paymentagentserver;

import java.util.List;
import java.util.UUID;

import com.github.cokelee777.paymentagentserver.executor.SkillExecutor;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.jsonrpc.common.wrappers.SendMessageResponse;
import io.a2a.spec.*;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for executing A2A Protocol requests on the payment agent.
 * <p>
 * This controller processes incoming A2A SEND_MESSAGE requests, routes them to the
 * appropriate skill executor, and returns task responses.
 * </p>
 */
@RestController
public class PaymentAgentExecutorProducer {

	private final List<SkillExecutor> skillExecutors;

	/**
	 * Constructs a PaymentAgentExecutorProducer with a list of skill executors.
	 * @param skillExecutors list of available skill executors
	 */
	public PaymentAgentExecutorProducer(List<SkillExecutor> skillExecutors) {
		this.skillExecutors = skillExecutors;
	}

	/**
	 * Handles A2A Protocol requests.
	 * <p>
	 * This endpoint accepts JSON-RPC requests with the SEND_MESSAGE method, extracts the
	 * message content, determines if it's an internal agent call, and routes it to an
	 * appropriate skill executor.
	 * </p>
	 * @param body the JSON-RPC request body
	 * @return a {@code ResponseEntity} containing the task response
	 * @throws JsonProcessingException if JSON processing fails
	 */
	@PostMapping(value = "/a2a", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> execute(@RequestBody String body) throws JsonProcessingException {
		JsonObject request = JsonParser.parseString(body).getAsJsonObject();
		Object requestId = extractId(request);
		String method = request.get("method").getAsString();

		if (!A2AMethods.SEND_MESSAGE_METHOD.equals(method)) {
			return ResponseEntity.ok(JsonUtil.toJson(new SendMessageResponse(requestId,
					new A2AError(A2AErrorCodes.METHOD_NOT_FOUND_ERROR_CODE, "Method not found: " + method, null))));
		}

		JsonObject params = request.getAsJsonObject("params");
		String userText = extractText(params);
		boolean isInternalCall = Message.Role.ROLE_AGENT.name().equals(extractRole(params));

		Task task;
		try {
			String resultText = routeToExecutor(userText, isInternalCall);
			task = Task.builder()
				.id(UUID.randomUUID().toString())
				.contextId(UUID.randomUUID().toString())
				.status(new TaskStatus(TaskState.TASK_STATE_COMPLETED))
				.artifacts(List.of(Artifact.builder()
					.artifactId(UUID.randomUUID().toString())
					.parts(new TextPart(resultText))
					.build()))
				.build();
		}
		catch (Exception e) {
			task = Task.builder()
				.id(UUID.randomUUID().toString())
				.contextId(UUID.randomUUID().toString())
				.status(new TaskStatus(TaskState.TASK_STATE_FAILED))
				.build();
		}

		return ResponseEntity.ok(JsonUtil.toJson(new SendMessageResponse(requestId, task)));
	}

	/**
	 * Routes the user text to an appropriate skill executor.
	 * @param userText the user message text
	 * @param isInternalCall whether this is an internal agent-to-agent call
	 * @return the result from the executor or a fallback error message
	 */
	private String routeToExecutor(String userText, boolean isInternalCall) {
		for (SkillExecutor executor : skillExecutors) {
			if (executor.canHandle(userText, isInternalCall)) {
				return executor.execute(userText, isInternalCall);
			}
		}
		return "결제 상태 조회는 주문번호(ORD-)를 포함해 주세요.";
	}

	/**
	 * Extracts the text message from the A2A Protocol request parameters.
	 * @param params the params object containing the message
	 * @return the extracted text, or an empty string if not found
	 */
	private String extractText(JsonObject params) {
		JsonObject message = params.getAsJsonObject("message");
		if (message == null)
			return "";
		JsonArray parts = message.getAsJsonArray("parts");
		if (parts == null)
			return "";
		for (var part : parts) {
			JsonObject partObj = part.getAsJsonObject();
			if (partObj.has(TextPart.TEXT)) {
				return partObj.get(TextPart.TEXT).getAsString();
			}
		}
		return "";
	}

	/**
	 * Extracts the request ID from the JSON-RPC request.
	 * @param request the JSON-RPC request object
	 * @return the request ID as an integer, string, or null
	 */
	private Object extractId(JsonObject request) {
		var idElement = request.get("id");
		if (idElement == null || idElement.isJsonNull())
			return null;
		var prim = idElement.getAsJsonPrimitive();
		if (prim.isNumber())
			return prim.getAsInt();
		return prim.getAsString();
	}

	/**
	 * Extracts the message role from the request parameters.
	 * @param params the params object
	 * @return the role string, or an empty string if not found
	 */
	private String extractRole(JsonObject params) {
		JsonObject message = params.getAsJsonObject("message");
		if (message == null)
			return "";
		var role = message.get("role");
		return role != null ? role.getAsString() : "";
	}

}
