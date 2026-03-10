package com.github.cokelee777.a2a.server;

import com.google.gson.JsonSyntaxException;
import io.a2a.grpc.utils.JSONRPCUtils;
import io.a2a.jsonrpc.common.json.IdJsonMappingException;
import io.a2a.jsonrpc.common.json.InvalidParamsJsonMappingException;
import io.a2a.jsonrpc.common.json.JsonMappingException;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.MethodNotFoundJsonMappingException;
import io.a2a.jsonrpc.common.wrappers.A2AErrorResponse;
import io.a2a.jsonrpc.common.wrappers.A2ARequest;
import io.a2a.jsonrpc.common.wrappers.A2AResponse;
import io.a2a.jsonrpc.common.wrappers.CancelTaskRequest;
import io.a2a.jsonrpc.common.wrappers.CancelTaskResponse;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.CreateTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.DeleteTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetExtendedAgentCardResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigRequest;
import io.a2a.jsonrpc.common.wrappers.GetTaskPushNotificationConfigResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskRequest;
import io.a2a.jsonrpc.common.wrappers.GetTaskResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksRequest;
import io.a2a.jsonrpc.common.wrappers.ListTasksResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksResult;
import io.a2a.jsonrpc.common.wrappers.NonStreamingJSONRPCRequest;
import io.a2a.jsonrpc.common.wrappers.SendMessageRequest;
import io.a2a.jsonrpc.common.wrappers.SendMessageResponse;
import io.a2a.jsonrpc.common.wrappers.SendStreamingMessageResponse;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.spec.A2AError;
import io.a2a.spec.EventKind;
import io.a2a.spec.InternalError;
import io.a2a.spec.JSONParseError;
import io.a2a.spec.Task;
import io.a2a.spec.TaskPushNotificationConfig;
import io.a2a.spec.UnsupportedOperationError;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Set;

/**
 * REST controller handling A2A Protocol JSON-RPC requests.
 *
 * <p>
 * Provides two endpoints:
 * </p>
 * <ul>
 * <li>{@code POST /a2a} — synchronous JSON-RPC dispatch for standard A2A methods</li>
 * <li>{@code POST /a2a/stream} — SSE streaming for {@code message/stream} requests</li>
 * </ul>
 *
 * <p>
 * Incoming request bodies are parsed via {@link JSONRPCUtils#parseRequestBody} and
 * dispatched to the appropriate {@link RequestHandler} method. Error conditions produce
 * JSON-RPC error responses.
 * </p>
 */
@RestController
@RequiredArgsConstructor
public class A2AJsonRpcController {

	private final RequestHandler requestHandler;

	/**
	 * Handles synchronous A2A Protocol JSON-RPC requests at {@code POST /a2a}.
	 *
	 * <p>
	 * Supported methods: {@code message/send}, {@code tasks/get}, {@code tasks/cancel},
	 * {@code tasks/list}, {@code tasks/pushNotification/create},
	 * {@code tasks/pushNotification/get}, {@code tasks/pushNotification/delete}.
	 * Streaming and unknown request types return an {@code UnsupportedOperationError}.
	 * Parsing and dispatch errors are mapped to typed JSON-RPC error responses
	 * ({@code -32600} through {@code -32603}) and returned with HTTP 500.
	 * </p>
	 * @param body the raw JSON-RPC request body
	 * @return a {@code ResponseEntity} containing the serialized JSON-RPC response
	 */
	@PostMapping(value = "/a2a", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> handle(@RequestBody String body) {
		ServerCallContext context = createCallContext();
		A2ARequest<?> request = null;
		A2AErrorResponse error = null;
		try {
			request = JSONRPCUtils.parseRequestBody(body, null);
			if (request instanceof NonStreamingJSONRPCRequest<?> nonStreamingRequest) {
				A2AResponse<?> nonStreamingResponse = processNonStreamingRequest(nonStreamingRequest, context);
				return ResponseEntity.ok(serializeResponse(nonStreamingResponse));
			}
		}
		catch (A2AError e) {
			error = new A2AErrorResponse(e);
		}
		catch (InvalidParamsJsonMappingException e) {
			error = new A2AErrorResponse(e.getId(), new io.a2a.spec.InvalidParamsError(null, e.getMessage(), null));
		}
		catch (MethodNotFoundJsonMappingException e) {
			error = new A2AErrorResponse(e.getId(), new io.a2a.spec.MethodNotFoundError(null, e.getMessage(), null));
		}
		catch (IdJsonMappingException e) {
			error = new A2AErrorResponse(e.getId(), new io.a2a.spec.InvalidRequestError(null, e.getMessage(), null));
		}
		catch (JsonMappingException e) {
			error = new A2AErrorResponse(new io.a2a.spec.InvalidRequestError(null, e.getMessage(), null));
		}
		catch (JsonSyntaxException | JsonProcessingException e) {
			error = new A2AErrorResponse(new JSONParseError(e.getMessage()));
		}
		catch (Throwable t) {
			error = new A2AErrorResponse(new InternalError(t.getMessage()));
		}

		if (error != null) {
			return ResponseEntity.internalServerError().body(serializeResponse(error));
		}

		return ResponseEntity.internalServerError()
			.body(serializeResponse(generateErrorResponse(request, new UnsupportedOperationError())));
	}

	/**
	 * Creates a new {@link ServerCallContext} with an unauthenticated user and no
	 * extensions.
	 * @return a default {@link ServerCallContext}
	 */
	private ServerCallContext createCallContext() {
		return new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of(), Set.of());
	}

	/**
	 * Dispatches a non-streaming JSON-RPC request to the appropriate
	 * {@link RequestHandler} method using pattern matching.
	 *
	 * <p>
	 * Returns an {@link io.a2a.spec.UnsupportedOperationError} response for request types
	 * that are not handled by this controller.
	 * </p>
	 * @param request the parsed non-streaming JSON-RPC request
	 * @param context the call context for the current request
	 * @return the corresponding {@link A2AResponse}
	 */
	private A2AResponse<?> processNonStreamingRequest(NonStreamingJSONRPCRequest<?> request,
			ServerCallContext context) {
		Object requestId = request.getId();
		if (request instanceof GetTaskRequest req) {
			Task task = requestHandler.onGetTask(req.getParams(), context);
			return new GetTaskResponse(requestId, task);
		}
		if (request instanceof CancelTaskRequest req) {
			Task task = requestHandler.onCancelTask(req.getParams(), context);
			return new CancelTaskResponse(requestId, task);
		}
		if (request instanceof ListTasksRequest req) {
			ListTasksResult listTasksResult = requestHandler.onListTasks(req.getParams(), context);
			return new ListTasksResponse(requestId, listTasksResult);
		}
		if (request instanceof CreateTaskPushNotificationConfigRequest req) {
			TaskPushNotificationConfig taskPushNotificationConfig = requestHandler
				.onCreateTaskPushNotificationConfig(req.getParams(), context);
			return new CreateTaskPushNotificationConfigResponse(requestId, taskPushNotificationConfig);
		}
		if (request instanceof GetTaskPushNotificationConfigRequest req) {
			TaskPushNotificationConfig taskPushNotificationConfig = requestHandler
				.onGetTaskPushNotificationConfig(req.getParams(), context);
			return new GetTaskPushNotificationConfigResponse(requestId, taskPushNotificationConfig);
		}
		if (request instanceof SendMessageRequest req) {
			EventKind result = requestHandler.onMessageSend(req.getParams(), context);
			return new SendMessageResponse(requestId, result);
		}
		if (request instanceof DeleteTaskPushNotificationConfigRequest req) {
			requestHandler.onDeleteTaskPushNotificationConfig(req.getParams(), context);
			return new DeleteTaskPushNotificationConfigResponse(request.getId());
		}
		return generateErrorResponse(request, new UnsupportedOperationError());
	}

	/**
	 * Builds an {@link A2AErrorResponse} for the given request ID and error.
	 * @param request the original JSON-RPC request whose ID is echoed in the error
	 * @param error the A2A error to include in the response
	 * @return an {@link A2AErrorResponse} wrapping the provided error
	 */
	private A2AResponse<?> generateErrorResponse(A2ARequest<?> request, A2AError error) {
		return new A2AErrorResponse(request.getId(), error);
	}

	/**
	 * Serializes an {@link A2AResponse} to a JSON-RPC response string.
	 *
	 * <p>
	 * Error responses are serialized directly via
	 * {@link JSONRPCUtils#toJsonRPCErrorResponse}. Successful responses are first
	 * converted to their Protobuf representation via {@link #convertToProto} and then
	 * serialized via {@link JSONRPCUtils#toJsonRPCResultResponse}.
	 * </p>
	 * @param response the response to serialize
	 * @return the JSON-RPC serialized string
	 */
	private static String serializeResponse(A2AResponse<?> response) {
		// For error responses, use Jackson serialization (errors are standardized)
		if (response instanceof A2AErrorResponse error) {
			return JSONRPCUtils.toJsonRPCErrorResponse(error.getId(), error.getError());
		}
		if (response.getError() != null) {
			return JSONRPCUtils.toJsonRPCErrorResponse(response.getId(), response.getError());
		}
		// Convert domain response to protobuf message and serialize
		com.google.protobuf.MessageOrBuilder protoMessage = convertToProto(response);
		return JSONRPCUtils.toJsonRPCResultResponse(response.getId(), protoMessage);
	}

	/**
	 * Converts a successful {@link A2AResponse} to its corresponding Protobuf
	 * {@link com.google.protobuf.MessageOrBuilder} for JSON-RPC serialization.
	 *
	 * <p>
	 * Each known response type is mapped to the appropriate {@code ProtoUtils.ToProto}
	 * conversion method. {@link DeleteTaskPushNotificationConfigResponse} produces an
	 * empty Protobuf message because the operation has no result body.
	 * </p>
	 * @param response the successful response to convert
	 * @return the Protobuf representation of the response result
	 * @throws IllegalArgumentException if the response type is not recognized
	 */
	private static com.google.protobuf.MessageOrBuilder convertToProto(A2AResponse<?> response) {
		if (response instanceof GetTaskResponse r) {
			return io.a2a.grpc.utils.ProtoUtils.ToProto.task(r.getResult());
		}
		else if (response instanceof CancelTaskResponse r) {
			return io.a2a.grpc.utils.ProtoUtils.ToProto.task(r.getResult());
		}
		else if (response instanceof SendMessageResponse r) {
			return io.a2a.grpc.utils.ProtoUtils.ToProto.taskOrMessage(r.getResult());
		}
		else if (response instanceof ListTasksResponse r) {
			return io.a2a.grpc.utils.ProtoUtils.ToProto.listTasksResult(r.getResult());
		}
		else if (response instanceof CreateTaskPushNotificationConfigResponse r) {
			return io.a2a.grpc.utils.ProtoUtils.ToProto.createTaskPushNotificationConfigResponse(r.getResult());
		}
		else if (response instanceof GetTaskPushNotificationConfigResponse r) {
			return io.a2a.grpc.utils.ProtoUtils.ToProto.getTaskPushNotificationConfigResponse(r.getResult());
		}
		else if (response instanceof DeleteTaskPushNotificationConfigResponse) {
			// DeleteTaskPushNotificationConfig has no result body, just return empty
			// message
			return com.google.protobuf.Empty.getDefaultInstance();
		}
		else if (response instanceof GetExtendedAgentCardResponse r) {
			return io.a2a.grpc.utils.ProtoUtils.ToProto.getExtendedCardResponse(r.getResult());
		}
		else if (response instanceof SendStreamingMessageResponse r) {
			return io.a2a.grpc.utils.ProtoUtils.ToProto.taskOrMessageStream(r.getResult());
		}
		else {
			throw new IllegalArgumentException("Unknown response type: " + response.getClass().getName());
		}
	}

}
