package com.github.cokelee777.a2aclient.orchestrator;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.stereotype.Service;

@Service
public class QueryAnalysisService {

    private static final String SYSTEM_PROMPT = """
        당신은 사용자 문의를 분류하는 라우터입니다.
        사용자 문의에서 의도(intent)와 필요한 식별자를 추출하세요.

        가능한 의도:
        - order_cancel: 주문 취소 (주문번호 ORD- 로 시작)
        - delivery_track: 배송 조회 (운송장번호 TRACK- 로 시작)
        - both: 주문 취소와 배송 조회를 모두 요청
        - unclear: 위에 해당하지 않거나 식별자를 알 수 없음

        응답은 반드시 아래 JSON 형식만 출력하세요. 다른 설명 없이 JSON만 출력합니다.
        {"intent":"order_cancel|delivery_track|both|unclear","orderNumber":"ORD-1001 또는 null","trackingNumber":"TRACK-1001 또는 null"}
        """;

    private final ChatModel chatModel;
    private final Gson gson = new Gson();

    public QueryAnalysisService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    public IntentAnalysis analyze(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return new IntentAnalysis(IntentAnalysis.INTENT_UNCLEAR, null, null);
        }

        try {
            String promptText = SYSTEM_PROMPT + "\n\n사용자 문의: " + userMessage;
            Prompt prompt = new Prompt(new UserMessage(promptText));
            var response = chatModel.call(prompt);
            if (response == null || response.getResult() == null || response.getResult().getOutput() == null) {
                return new IntentAnalysis(IntentAnalysis.INTENT_UNCLEAR, null, null);
            }
            String content = response.getResult().getOutput().getText();
            if (content == null || content.isBlank()) {
                return new IntentAnalysis(IntentAnalysis.INTENT_UNCLEAR, null, null);
            }

            String json = content.trim();
            if (json.startsWith("```")) {
                int start = json.indexOf("{");
                int end = json.lastIndexOf("}") + 1;
                if (start >= 0 && end > start) {
                    json = json.substring(start, end);
                }
            }

            try {
                IntentAnalysis parsed = gson.fromJson(json, IntentAnalysis.class);
                return parsed != null ? normalize(parsed) : new IntentAnalysis(IntentAnalysis.INTENT_UNCLEAR, null, null);
            } catch (JsonSyntaxException e) {
                return new IntentAnalysis(IntentAnalysis.INTENT_UNCLEAR, null, null);
            }
        } catch (Exception e) {
            return new IntentAnalysis(IntentAnalysis.INTENT_UNCLEAR, null, null);
        }
    }

    private static IntentAnalysis normalize(IntentAnalysis a) {
        String intent = a.intent();
        if (intent == null || intent.isBlank()) intent = IntentAnalysis.INTENT_UNCLEAR;
        String order = a.orderNumber();
        String track = a.trackingNumber();
        if (order != null && (order.equals("null") || order.isBlank())) order = null;
        if (track != null && (track.equals("null") || track.isBlank())) track = null;
        return new IntentAnalysis(intent, order, track);
    }
}
