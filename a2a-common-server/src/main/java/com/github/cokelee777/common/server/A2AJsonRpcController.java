package com.github.cokelee777.common.server;

import io.a2a.grpc.utils.JSONRPCUtils;
import io.a2a.jsonrpc.common.json.IdJsonMappingException;
import io.a2a.jsonrpc.common.json.JsonMappingException;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.jsonrpc.common.wrappers.A2AErrorResponse;
import io.a2a.jsonrpc.common.wrappers.A2ARequest;
import io.a2a.jsonrpc.common.wrappers.CancelTaskRequest;
import io.a2a.jsonrpc.common.wrappers.CancelTaskResponse;
import io.a2a.jsonrpc.common.wrappers.GetTaskRequest;
import io.a2a.jsonrpc.common.wrappers.GetTaskResponse;
import io.a2a.jsonrpc.common.wrappers.ListTasksRequest;
import io.a2a.jsonrpc.common.wrappers.ListTasksResponse;
import io.a2a.jsonrpc.common.wrappers.SendMessageRequest;
import io.a2a.jsonrpc.common.wrappers.SendMessageResponse;
import io.a2a.jsonrpc.common.wrappers.SendStreamingMessageRequest;
import io.a2a.jsonrpc.common.wrappers.SendStreamingMessageResponse;
import io.a2a.server.ServerCallContext;
import io.a2a.server.auth.UnauthenticatedUser;
import io.a2a.server.requesthandlers.RequestHandler;
import io.a2a.server.util.sse.SseFormatter;
import io.a2a.spec.A2AError;
import io.a2a.spec.A2AErrorCodes;
import io.a2a.spec.A2AMethods;
import io.a2a.spec.StreamingEventKind;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.Flow;
import java.util.concurrent.atomic.AtomicLong;

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
	 * {@code tasks/list}. Unknown methods receive a method-not-found error response.
	 * </p>
	 * @param body the raw JSON-RPC request body
	 * @return a {@code ResponseEntity} containing the serialized JSON-RPC response
	 * @throws JsonProcessingException if serialization of the response fails
	 */
	@PostMapping(value = "/a2a", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<String> handle(@RequestBody String body) throws JsonProcessingException {
		ServerCallContext ctx = createCallContext();
		A2ARequest<?> request;
		try {
			request = JSONRPCUtils.parseRequestBody(body, null);
		}
		catch (IdJsonMappingException ex) {
			A2AErrorResponse errorResponse = new A2AErrorResponse(ex.getId(),
					new A2AError(A2AErrorCodes.INVALID_REQUEST_ERROR_CODE, ex.getMessage(), null));
			return ResponseEntity.ok(JsonUtil.toJson(errorResponse));
		}
		catch (JsonMappingException ex) {
			A2AErrorResponse errorResponse = new A2AErrorResponse(
					new A2AError(A2AErrorCodes.JSON_PARSE_ERROR_CODE, ex.getMessage(), null));
			return ResponseEntity.ok(JsonUtil.toJson(errorResponse));
		}

		String method = request.getMethod();
		Object requestId = request.getId();

		try {
			if (A2AMethods.SEND_MESSAGE_METHOD.equals(method)) {
				SendMessageRequest sendMessageRequest = (SendMessageRequest) request;
				io.a2a.spec.EventKind result = requestHandler.onMessageSend(sendMessageRequest.getParams(), ctx);
				return ResponseEntity.ok(JsonUtil.toJson(new SendMessageResponse(requestId, result)));
			}
			else if (A2AMethods.GET_TASK_METHOD.equals(method)) {
				GetTaskRequest getTaskRequest = (GetTaskRequest) request;
				io.a2a.spec.Task task = requestHandler.onGetTask(getTaskRequest.getParams(), ctx);
				return ResponseEntity.ok(JsonUtil.toJson(new GetTaskResponse(requestId, task)));
			}
			else if (A2AMethods.CANCEL_TASK_METHOD.equals(method)) {
				CancelTaskRequest cancelTaskRequest = (CancelTaskRequest) request;
				io.a2a.spec.Task task = requestHandler.onCancelTask(cancelTaskRequest.getParams(), ctx);
				return ResponseEntity.ok(JsonUtil.toJson(new CancelTaskResponse(requestId, task)));
			}
			else if (A2AMethods.LIST_TASK_METHOD.equals(method)) {
				ListTasksRequest listTasksRequest = (ListTasksRequest) request;
				io.a2a.jsonrpc.common.wrappers.ListTasksResult result = requestHandler
					.onListTasks(listTasksRequest.getParams(), ctx);
				return ResponseEntity.ok(JsonUtil.toJson(new ListTasksResponse(requestId, result)));
			}
			else {
				A2AErrorResponse errorResponse = new A2AErrorResponse(requestId,
						new A2AError(A2AErrorCodes.METHOD_NOT_FOUND_ERROR_CODE, "Method not found: " + method, null));
				return ResponseEntity.ok(JsonUtil.toJson(errorResponse));
			}
		}
		catch (A2AError ex) {
			return ResponseEntity.ok(JsonUtil.toJson(new A2AErrorResponse(requestId, ex)));
		}
		catch (Exception ex) {
			A2AErrorResponse errorResponse = new A2AErrorResponse(requestId,
					new A2AError(A2AErrorCodes.INTERNAL_ERROR_CODE, ex.getMessage(), null));
			return ResponseEntity.ok(JsonUtil.toJson(errorResponse));
		}
	}

	/**
	 * Handles streaming A2A Protocol requests at {@code POST /a2a/stream} using
	 * Server-Sent Events.
	 *
	 * <p>
	 * Parses the request as a {@code message/stream} request, invokes
	 * {@link RequestHandler#onMessageSendStream}, and pushes each
	 * {@link StreamingEventKind} event as an SSE frame to the client.
	 * </p>
	 * @param body the raw JSON-RPC streaming request body
	 * @return a {@link SseEmitter} that streams JSON-RPC response events
	 * @throws JsonProcessingException if response serialization fails
	 */
	@PostMapping(value = "/a2a/stream", consumes = MediaType.APPLICATION_JSON_VALUE,
			produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	public SseEmitter stream(@RequestBody String body) throws JsonProcessingException {
		SseEmitter emitter = new SseEmitter(0L);
		ServerCallContext ctx = createCallContext();

		A2ARequest<?> request;
		try {
			request = JSONRPCUtils.parseRequestBody(body, null);
		}
		catch (JsonMappingException ex) {
			Object errorId = (ex instanceof IdJsonMappingException idEx) ? idEx.getId() : null;
			A2AErrorResponse errorResponse = new A2AErrorResponse(errorId,
					new A2AError(A2AErrorCodes.INVALID_REQUEST_ERROR_CODE, ex.getMessage(), null));
			try {
				emitter.send(SseEmitter.event().data(JsonUtil.toJson(errorResponse)));
			}
			catch (Exception ignored) {
			}
			emitter.complete();
			return emitter;
		}

		Object requestId = request.getId();

		if (!(request instanceof SendStreamingMessageRequest sendStreamingRequest)) {
			A2AErrorResponse errorResponse = new A2AErrorResponse(requestId, new A2AError(
					A2AErrorCodes.METHOD_NOT_FOUND_ERROR_CODE, "Method not found: " + request.getMethod(), null));
			try {
				emitter.send(SseEmitter.event().data(JsonUtil.toJson(errorResponse)));
			}
			catch (Exception ignored) {
			}
			emitter.complete();
			return emitter;
		}

		AtomicLong eventIndex = new AtomicLong(0);
		Flow.Publisher<StreamingEventKind> publisher;
		try {
			publisher = requestHandler.onMessageSendStream(sendStreamingRequest.getParams(), ctx);
		}
		catch (A2AError ex) {
			A2AErrorResponse errorResponse = new A2AErrorResponse(requestId, ex);
			try {
				emitter.send(SseEmitter.event().data(JsonUtil.toJson(errorResponse)));
			}
			catch (Exception ignored) {
			}
			emitter.complete();
			return emitter;
		}

		publisher.subscribe(new Flow.Subscriber<>() {
			@Override
			public void onSubscribe(Flow.Subscription subscription) {
				subscription.request(Long.MAX_VALUE);
			}

			@Override
			public void onNext(StreamingEventKind item) {
				try {
					SendStreamingMessageResponse response = new SendStreamingMessageResponse(requestId, item);
					String sseData = SseFormatter.formatResponseAsSSE(response, eventIndex.getAndIncrement());
					emitter.send(SseEmitter.event().data(sseData));
				}
				catch (Exception ex) {
					emitter.completeWithError(ex);
				}
			}

			@Override
			public void onError(Throwable throwable) {
				emitter.completeWithError(throwable);
			}

			@Override
			public void onComplete() {
				emitter.complete();
			}
		});

		return emitter;
	}

	/**
	 * Creates a new {@link ServerCallContext} with an unauthenticated user and no
	 * extensions.
	 * @return a default {@link ServerCallContext}
	 */
	private ServerCallContext createCallContext() {
		return new ServerCallContext(UnauthenticatedUser.INSTANCE, Map.of(), Set.of());
	}

}
