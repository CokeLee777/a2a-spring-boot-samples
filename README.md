# a2a-spring-boot-samples
Spring Boot samples using the Agent2Agent (A2A) Protocol

## 모듈 구성

| 모듈 | 포트 | 설명 |
|------|------|------|
| **a2a-server/a2a-order-server** | 8081 | 주문 취소 가능 여부 확인 에이전트 (ORD-* 처리) |
| **a2a-server/a2a-delivery-server** | 8082 | 배송 조회 에이전트 (TRACK-* 처리) |
| **a2a-server/a2a-payment-server** | 8083 | 결제·환불 상태 에이전트 (주문 취소 시 환불 가능 여부 조회) |
| **a2a-client** | 8080 | 진입점 + LLM 의도 분석 → 해당 에이전트 호출 |

## 에이전트 간 통신 (A2A Java SDK)

에이전트 간에는 **A2A 프로토콜**로만 통신합니다. 각 에이전트에 `a2a-java-sdk-client`를 넣고, 상대 에이전트의 Agent Card를 resolve한 뒤 `Client.sendMessage()`로 메시지를 보내고, 응답 Task의 artifact 텍스트를 파싱해 사용합니다.

### 주문 취소 가능 여부 확인 시

Order Agent는 다음 두 에이전트를 **병렬 호출**합니다:

- **Delivery Agent**
    - `[A2A-INTERNAL] delivery-status TRACK-xxx`
    - 배송 상태가 `배송중` 또는 `배송완료`이면 취소 불가

- **Payment Agent**
    - `[A2A-INTERNAL] payment-status ORD-xxx`
    - 환불 불가 상태이면 취소 불가

두 결과를 종합하여 최종 취소 가능 여부를 판단합니다.

## 배송 조회 시

Delivery Agent는 주문 정보를 함께 보여주기 위해\
Order Agent에 내부 A2A 메시지를 보냅니다.

    [A2A-INTERNAL] order-info TRACK-xxx

Order Agent는 운송장번호로 주문을 조회한 뒤, 다음과 같은 구조의 텍스트
응답을 반환합니다:

    orderNumber: ORD-1001
    orderDate: 2026-02-27
    status: PAID

Delivery Agent는 해당 응답을 파싱하여 배송 상태와 함께 주문 정보를
결합해 최종 응답을 생성합니다.


## 실행 방법

## 실행 방법

1.  Order Agent 실행:

        ./gradlew :a2a-server:a2a-order-server:bootRun

2.  Delivery Agent 실행:

        ./gradlew :a2a-server:a2a-delivery-server:bootRun

3.  Payment Agent 실행:

        ./gradlew :a2a-server:a2a-payment-server:bootRun

4.  Client 실행:

        ./gradlew :a2a-client:bootRun

## 직접 호출 (기존 방식)

-   배송 조회:

        http://localhost:8080/api/delivery?trackingNumber=TRACK-1001

-   주문 취소 가능 여부 확인:

        http://localhost:8080/api/order/cancel/check?orderNumber=ORD-1001

## 자유 문의 (LLM 라우팅)

Client는 Spring AI + LLM을 사용하여\
사용자 문의의 의도(intent)와 식별자를 분석한 뒤, 해당 A2A 에이전트를
호출합니다.

### 분석 가능한 의도

-   `order_cancellability_check`
-   `delivery_track`
-   `both`
-   `unclear`

## 필수 환경 변수 (Client 실행 전)

- `OPENAI_API_KEY`: OPENAI API 키(OpenAI, Gemini 등)

## 요청 예

### 주문 취소 가능 여부 확인

``` bash
curl -X POST http://localhost:8080/api/chat   -H "Content-Type: application/json"   -d '{"message": "ORD-1001 취소 가능해?"}'
```

### 배송 조회

``` bash
curl -X POST http://localhost:8080/api/chat   -H "Content-Type: application/json"   -d '{"message": "TRACK-1001 배송 어디쯤이야?"}'
```

## 전체 흐름

    사용자 문의
       ↓
    LLM (의도 + 식별자 추출)
       ↓
    A2A Client 라우팅
       ↓
    Order / Delivery Agent 호출
       ↓
    필요 시 내부 A2A 병렬 호출
       ↓
    최종 응답 반환

## 특징

-   LLM은 실제 취소를 실행하지 않음
-   Order Agent가 취소 가능 여부만 판단
-   에이전트 간 통신은 HTTP 직접 호출이 아닌 **A2A 프로토콜**
-   내부 병렬 호출을 통한 마이크로 에이전트 협력 구조
