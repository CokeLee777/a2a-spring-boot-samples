# a2a-spring-boot-samples
Spring Boot samples using the Agent2Agent (A2A) Protocol

## 모듈 구성

| 모듈 | 포트 | 설명 |
|------|------|------|
| **a2a-server/a2a-order-server** | 8082 | 주문 취소 에이전트 (ORD-* 처리) |
| **a2a-server/a2a-delivery-server** | 8083 | 배송 조회 에이전트 (TRACK-* 처리) |
| **a2a-client** | 8081 | 두 에이전트를 호출하는 클라이언트 |

## 에이전트 간 통신 (A2A Java SDK)

에이전트 간에는 **A2A 프로토콜**로만 통신합니다. 각 에이전트에 `a2a-java-sdk-client`를 넣고, 상대 에이전트의 Agent Card를 resolve한 뒤 `Client.sendMessage()`로 메시지를 보내고, 응답 Task의 artifact 텍스트를 파싱해 사용합니다.

- **주문 취소 시**: Order Agent가 Delivery Agent에게 `[A2A-INTERNAL] delivery-status TRACK-xxx` 메시지를 보냅니다. 배송 에이전트는 `status:배송중` 형태로만 응답하고, 주문 에이전트는 이 값을 보고 배송중/배송완료면 취소 불가 메시지를 반환합니다.
- **배송 조회 시**: Delivery Agent가 Order Agent에게 `[A2A-INTERNAL] order-info TRACK-xxx` 메시지를 보냅니다. 주문 에이전트는 `orderNumber:...`, `orderDate:...`, `status:...` 등 한 줄씩 응답하고, 배송 에이전트는 이걸 파싱해 조회 결과 아래에 주문 연동 정보를 붙여 반환합니다.

## 실행 방법

1. Order Agent 실행: `./gradlew :a2a-server:a2a-order-server:bootRun`
2. Delivery Agent 실행: `./gradlew :a2a-server:a2a-delivery-server:bootRun` (별도 터미널)
3. Client 실행: `./gradlew :a2a-client:bootRun` (별도 터미널)

- 배송 조회: `http://localhost:8081/api/delivery?trackingNumber=TRACK-1001`
- 주문 취소: `http://localhost:8081/api/order/cancel?orderNumber=ORD-1001`
