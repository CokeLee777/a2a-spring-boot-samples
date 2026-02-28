package com.github.cokelee777.deliveryagentserver;

import java.util.List;
import java.util.UUID;

import com.github.cokelee777.deliveryagentserver.executor.SkillExecutor;
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

@RestController
public class JsonRpcController {

    private final List<SkillExecutor> skillExecutors;

    public JsonRpcController(List<SkillExecutor> skillExecutors) {
        this.skillExecutors = skillExecutors;
    }

    @PostMapping(value = "/a2a", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> handleJsonRpc(@RequestBody String body) throws JsonProcessingException {
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
                    .artifacts(List.of(
                            Artifact.builder()
                                    .artifactId(UUID.randomUUID().toString())
                                    .parts(new TextPart(resultText))
                                    .build()
                    ))
                    .build();
        } catch (Exception e) {
            task = Task.builder()
                    .id(UUID.randomUUID().toString())
                    .contextId(UUID.randomUUID().toString())
                    .status(new TaskStatus(TaskState.TASK_STATE_FAILED))
                    .build();
        }

        return ResponseEntity.ok(JsonUtil.toJson(new SendMessageResponse(requestId, task)));
    }

    private String routeToExecutor(String userText, boolean isInternalCall) {
        for (SkillExecutor executor : skillExecutors) {
            if (executor.canHandle(userText, isInternalCall)) {
                return executor.execute(userText, isInternalCall);
            }
        }
        return "배송 조회는 운송장번호(TRACK-)를 포함해 주세요. 예: TRACK-1001 배송 조회해줘";
    }

    private String extractText(JsonObject params) {
        JsonObject message = params.getAsJsonObject("message");
        if (message == null) return "";
        JsonArray parts = message.getAsJsonArray("parts");
        if (parts == null) return "";
        for (var part : parts) {
            JsonObject partObj = part.getAsJsonObject();
            if (partObj.has(TextPart.TEXT)) {
                return partObj.get(TextPart.TEXT).getAsString();
            }
        }
        return "";
    }

    private Object extractId(JsonObject request) {
        var idElement = request.get("id");
        if (idElement == null || idElement.isJsonNull()) return null;
        var prim = idElement.getAsJsonPrimitive();
        if (prim.isNumber()) return prim.getAsInt();
        return prim.getAsString();
    }

    private String extractRole(JsonObject params) {
        JsonObject message = params.getAsJsonObject("message");
        if (message == null) return "";
        var role = message.get("role");
        return role != null ? role.getAsString() : "";
    }
}
