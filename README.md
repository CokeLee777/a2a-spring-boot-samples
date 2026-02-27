# a2a-spring-boot-samples
Spring Boot samples using the Agent2Agent (A2A) Protocol

## 모듈 구성

| 모듈 | 포트 | 설명 |
|------|------|------|
| **a2a-server/a2a-order-server** | 8081 | 주문 취소 에이전트 (ORD-* 처리) |
| **a2a-server/a2a-delivery-server** | 8082 | 배송 조회 에이전트 (TRACK-* 처리) |
| **a2a-server/a2a-payment-server** | 8083 | 결제·환불 상태 에이전트 (주문 취소 시 환불 가능 여부 조회) |
| **a2a-client** | 8080 | 진입점 + LLM 의도 분석 → 해당 에이전트 호출 |

## 에이전트 간 통신 (A2A Java SDK)

에이전트 간에는 **A2A 프로토콜**로만 통신합니다. 각 에이전트에 `a2a-java-sdk-client`를 넣고, 상대 에이전트의 Agent Card를 resolve한 뒤 `Client.sendMessage()`로 메시지를 보내고, 응답 Task의 artifact 텍스트를 파싱해 사용합니다.

- **주문 취소 시**: Order Agent가 **배송 에이전트**와 **결제 에이전트**를 **병렬**로 호출합니다. `[A2A-INTERNAL] delivery-status TRACK-xxx`로 배송 상태, `[A2A-INTERNAL] payment-status ORD-xxx`로 환불 가능 여부를 조회한 뒤, 배송중/배송완료이거나 환불 불가이면 취소 불가 메시지를 반환합니다.
- **배송 조회 시**: Delivery Agent가 Order Agent에게 `[A2A-INTERNAL] order-info TRACK-xxx` 메시지를 보냅니다. 주문 에이전트는 `orderNumber:...`, `orderDate:...`, `status:...` 등 한 줄씩 응답하고, 배송 에이전트는 이걸 파싱해 조회 결과 아래에 주문 연동 정보를 붙여 반환합니다.

## 실행 방법

1. Order Agent 실행: `./gradlew :a2a-server:a2a-order-server:bootRun`
2. Delivery Agent 실행: `./gradlew :a2a-server:a2a-delivery-server:bootRun` (별도 터미널)
3. Payment Agent 실행: `./gradlew :a2a-server:a2a-payment-server:bootRun` (별도 터미널)
4. Client 실행: `./gradlew :a2a-client:bootRun` (별도 터미널)

### 직접 호출 (기존)
- 배송 조회: `http://localhost:8080/api/delivery?trackingNumber=TRACK-1001`
- 주문 취소: `http://localhost:8080/api/order/cancel?orderNumber=ORD-1001`

### 자유 문의 (LLM 라우팅)
Client가 **Spring AI(OpenAI 호환)** 로 사용자 문의 의도를 분석한 뒤, 해당 A2A 에이전트를 호출합니다.

**필수 환경 변수 (Client 실행 전):**
- `OPENAI_API_KEY`: OpenAI API 키 (또는 OpenAI 호환 서비스 키)
- Ollama 등 로컬 서버 사용 시: `OPENAI_BASE_URL=http://localhost:11434/v1` 등으로 설정

**요청 예:**
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "ORD-1001 주문 취소해줘"}'
```
```bash
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "TRACK-1001 배송 어디쯤이야?"}'
```

**흐름:** 사용자 문의 → LLM 의도/엔티티 분석 → Order/Delivery 에이전트 A2A 호출 → 결과 반환
