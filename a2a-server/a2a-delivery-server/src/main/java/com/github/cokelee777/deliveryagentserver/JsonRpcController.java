package com.github.cokelee777.deliveryagentserver;

import java.util.List;
import java.util.UUID;

import com.github.cokelee777.deliveryagentserver.executor.SkillExecutor;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import io.a2a.jsonrpc.common.json.JsonProcessingException;
import io.a2a.jsonrpc.common.json.JsonUtil;
import io.a2a.spec.A2AMethods;
import io.a2a.spec.Artifact;
import io.a2a.spec.Task;
import io.a2a.spec.TaskState;
import io.a2a.spec.TaskStatus;
import io.a2a.spec.TextPart;
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
        JsonElement requestId = request.get("id");
        String method = request.get("method").getAsString();

        if (!A2AMethods.SEND_MESSAGE_METHOD.equals(method)) {
            return ResponseEntity.ok(buildErrorResponse(requestId, -32601, "Method not found: " + method));
        }

        JsonObject params = request.getAsJsonObject("params");
        String userText = extractText(params);

        String resultText = routeToExecutor(userText);

        Task task = Task.builder()
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

        String taskJson = JsonUtil.toJson(task);

        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", requestId);
        response.add("result", JsonParser.parseString(taskJson));

        return ResponseEntity.ok(response.toString());
    }

    private String routeToExecutor(String userText) {
        for (SkillExecutor executor : skillExecutors) {
            if (executor.canHandle(userText)) {
                return executor.execute(userText);
            }
        }
        return "배송 조회는 운송장번호(TRACK-)를 포함해 주세요. 예: TRACK-1001 배송 조회해줘";
    }

    private String extractText(JsonObject params) {
        JsonObject message = params.getAsJsonObject("message");
        JsonArray parts = message.getAsJsonArray("parts");
        for (JsonElement partEl : parts) {
            JsonObject part = partEl.getAsJsonObject();
            if (part.has("text")) {
                return part.get("text").getAsString();
            }
        }
        return "";
    }

    private String buildErrorResponse(JsonElement requestId, int code, String message) {
        JsonObject error = new JsonObject();
        error.addProperty("code", code);
        error.addProperty("message", message);

        JsonObject response = new JsonObject();
        response.addProperty("jsonrpc", "2.0");
        response.add("id", requestId);
        response.add("error", error);

        return response.toString();
    }
}
