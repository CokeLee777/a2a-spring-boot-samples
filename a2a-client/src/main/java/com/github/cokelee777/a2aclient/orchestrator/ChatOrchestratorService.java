package com.github.cokelee777.a2aclient.orchestrator;

import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.function.FunctionToolCallback;
import org.springframework.stereotype.Service;

@Service
public class ChatOrchestratorService {

    record OrderRequest(String orderNumber) {}
    record DeliveryRequest(String trackingNumber) {}

    private final ChatModel chatModel;
    private final ToolCallback[] tools;

    public ChatOrchestratorService(AgentInvokerService agentInvokerService, ChatModel chatModel) {
        this.chatModel = chatModel;
        this.tools = new ToolCallback[]{
            FunctionToolCallback
                .builder("order_cancellability_check",
                    (OrderRequest req) -> agentInvokerService.callOrderAgent(req.orderNumber()))
                .description("주문 취소 가능 여부 확인. 주문번호(ORD-xxxx)가 필요합니다.")
                .inputType(OrderRequest.class)
                .build(),
            FunctionToolCallback
                .builder("delivery_track",
                    (DeliveryRequest req) -> agentInvokerService.callDeliveryAgent(req.trackingNumber()))
                .description("배송 조회. 운송장번호(TRACK-xxxx)가 필요합니다.")
                .inputType(DeliveryRequest.class)
                .build()
        };
    }

    public String handleUserQuery(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return "문의 내용을 입력해 주세요.";
        }

        try {
            OpenAiChatOptions options = OpenAiChatOptions.builder()
                    .toolCallbacks(tools)
                    .internalToolExecutionEnabled(true)
                    .parallelToolCalls(true)
                    .build();

            Prompt prompt = new Prompt(new UserMessage(userMessage), options);
            ChatResponse response = chatModel.call(prompt);
            String content = response.getResult().getOutput().getText();
            return content != null && !content.isBlank() ? content.trim()
                    : "주문 취소는 주문번호(ORD-), 배송 조회는 운송장번호(TRACK-)를 포함해 주세요.";
        } catch (Exception e) {
            return "처리 중 오류가 발생했습니다. 잠시 후 다시 시도해 주세요.";
        }
    }
}
