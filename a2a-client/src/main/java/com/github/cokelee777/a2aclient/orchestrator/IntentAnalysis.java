package com.github.cokelee777.a2aclient.orchestrator;

import com.google.gson.annotations.SerializedName;

/**
 * LLM 의도 분석 결과.
 * intent: order_cancel | delivery_track | both | unclear
 * orderNumber: ORD-xxxx (주문 취소 시)
 * trackingNumber: TRACK-xxxx (배송 조회 시)
 */
public record IntentAnalysis(
        String intent,
        @SerializedName("orderNumber") String orderNumber,
        @SerializedName("trackingNumber") String trackingNumber
) {
    public static final String INTENT_ORDER_CANCEL = "order_cancel";
    public static final String INTENT_DELIVERY_TRACK = "delivery_track";
    public static final String INTENT_BOTH = "both";
    public static final String INTENT_UNCLEAR = "unclear";

    public boolean needsOrderAgent() {
        return INTENT_ORDER_CANCEL.equals(intent) || INTENT_BOTH.equals(intent);
    }

    public boolean needsDeliveryAgent() {
        return INTENT_DELIVERY_TRACK.equals(intent) || INTENT_BOTH.equals(intent);
    }
}
