package com.github.cokelee777.a2aclient.orchestrator;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Service
public class ChatOrchestratorService {

    private static final String CLARIFY_PROMPT = """
            사용자 문의에 주문/배송 관련 정보가 부족하거나 요청을 파악할 수 없습니다.
            아래 중 해당하는 안내를 짧고 친절하게 한 문장으로만 답하세요. 다른 설명 없이 안내 문장만 출력하세요.
            - 주문 취소를 원하시면 주문번호(예: ORD-1001)를 알려주세요.
            - 배송 조회를 원하시면 운송장번호(예: TRACK-1001)를 알려주세요.
            - 주문/배송 외 문의는 고객센터(1588-xxxx)로 연락해 주세요.
            """;

    private final QueryAnalysisService queryAnalysisService;
    private final AgentInvokerService agentInvokerService;
    private final ChatModel chatModel;

    public ChatOrchestratorService(
            QueryAnalysisService queryAnalysisService,
            AgentInvokerService agentInvokerService,
            ChatModel chatModel) {
        this.queryAnalysisService = queryAnalysisService;
        this.agentInvokerService = agentInvokerService;
        this.chatModel = chatModel;
    }

    public String handleUserQuery(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "문의 내용을 입력해 주세요.";
        }

        IntentAnalysis analysis = queryAnalysisService.analyze(userMessage);
        if (IntentAnalysis.INTENT_UNCLEAR.equals(analysis.intent())) {
            return formatUnclearResponse();
        }

        if (analysis.needsOrderAgent() && (analysis.orderNumber() == null || analysis.orderNumber().isBlank())) {
            return "주문 취소를 원하시면 주문번호(예: ORD-1001)를 알려주세요.";
        }
        if (analysis.needsDeliveryAgent() && (analysis.trackingNumber() == null || analysis.trackingNumber().isBlank())) {
            return "배송 조회를 원하시면 운송장번호(예: TRACK-1001)를 알려주세요.";
        }

        boolean needOrder = analysis.needsOrderAgent() && analysis.orderNumber() != null;
        boolean needDelivery = analysis.needsDeliveryAgent() && analysis.trackingNumber() != null;

        if (needOrder && needDelivery) {
            // A2A에 맞게 두 에이전트를 독립적으로 병렬 호출
            CompletableFuture<String> orderFuture = CompletableFuture.supplyAsync(() ->
                    agentInvokerService.callOrderAgent(analysis.orderNumber()));
            CompletableFuture<String> deliveryFuture = CompletableFuture.supplyAsync(() ->
                    agentInvokerService.callDeliveryAgent(analysis.trackingNumber()));

            CompletableFuture<String> combined = orderFuture.thenCombine(deliveryFuture,
                    (orderResult, deliveryResult) -> orderResult + "\n\n---\n\n" + deliveryResult);

            try {
                return combined.get(20, TimeUnit.SECONDS);
            } catch (Exception e) {
                return "에이전트 호출 중 오류: " + (e.getCause() != null ? e.getCause().getMessage() : e.getMessage());
            }
        }

        StringBuilder response = new StringBuilder();
        if (needOrder) {
            response.append(agentInvokerService.callOrderAgent(analysis.orderNumber()));
        }
        if (needDelivery) {
            if (!response.isEmpty()) response.append("\n\n---\n\n");
            response.append(agentInvokerService.callDeliveryAgent(analysis.trackingNumber()));
        }

        return !response.isEmpty() ? response.toString() : formatUnclearResponse();
    }

    private String formatUnclearResponse() {
        try {
            var response = chatModel.call(new Prompt(new UserMessage(CLARIFY_PROMPT)));

            String content = response.getResult().getOutput().getText();
            if (content != null && !content.isBlank()) {
                return content.trim();
            }
        } catch (Exception ignored) {
            // LLM 실패 시 기본 안내 문구 사용
        }
        return "주문 취소는 주문번호(ORD-), 배송 조회는 운송장번호(TRACK-)를 포함해 주세요.";
    }
}
