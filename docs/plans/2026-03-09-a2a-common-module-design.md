# a2a-common 모듈 도입 설계

## 배경

`task.artifacts()`에서 텍스트를 추출하는 아래 로직이 4곳에 동일하게 중복되어 있다.

- `a2a-server/a2a-order-server/.../A2aPaymentAgentClient.java`
- `a2a-server/a2a-order-server/.../A2aDeliveryAgentClient.java`
- `a2a-server/a2a-delivery-server/.../A2aOrderAgentClient.java`
- `a2a-client/.../A2aTool.java`

```java
StringBuilder sb = new StringBuilder();
if (task.artifacts() != null) {
    task.artifacts().forEach(artifact -> artifact.parts().forEach(part -> {
        if (part instanceof TextPart textPart) {
            sb.append(textPart.text());
        }
    }));
}
```

기존 `MessageUtil`(`a2a-spring-boot-server`)의 `extractTextFromMessage(Message)`와 동일한 패턴이지만,
`a2a-client`는 `a2a-spring-boot-server`를 의존하지 않아 공유가 불가능했다.

향후 클라이언트·서버 공통으로 필요한 SDK 유틸이 더 생길 것으로 예상되어, 공통 모듈을 별도 분리하기로 한다.

## 목표

- 중복된 artifact 텍스트 추출 로직을 단일 유틸 메서드로 통합
- `a2a-client`와 서버 모듈이 함께 사용 가능한 공통 모듈 도입
- 향후 SDK 관련 공통 유틸의 집합소 역할

## 모듈 구조

```
a2a-common (신규)
  ├── plugin: java-library
  ├── 의존: io.github.a2asdk:a2a-java-sdk-client
  └── 패키지: com.github.cokelee777.a2a.common.utils
              └── MessageUtil
                    ├── extractTextFromMessage(Message) ← a2a-spring-boot-server에서 이동
                    └── extractTextFromTask(Task)        ← 신규 추가
```

## 의존성 변경

| 모듈 | 변경 내용 |
|------|-----------|
| `a2a-common` | 신규 생성. `a2a-java-sdk-client` 의존 |
| `a2a-spring-boot-server` | `a2a-common` 의존 추가. 기존 `MessageUtil` 삭제 |
| `a2a-client` | `a2a-common` 의존 추가 |
| `a2a-server/*` | `a2a-spring-boot-server` 경유로 `a2a-common` 자동 상속 (변경 없음) |

## 신규 API

```java
package com.github.cokelee777.a2a.common.utils;

public class MessageUtil {

    /**
     * Extracts the plain text content from a {@link Message}.
     */
    public static String extractTextFromMessage(Message message) { ... }

    /**
     * Extracts the plain text content from all artifacts of a {@link Task}.
     */
    public static String extractTextFromTask(Task task) { ... }
}
```

## 적용 범위

중복 코드를 `MessageUtil.extractTextFromTask(task)`로 교체:

- `A2aPaymentAgentClient`
- `A2aDeliveryAgentClient`
- `A2aOrderAgentClient`
- `A2aTool`

## 검증 기준

- `./gradlew build` 성공
- `./gradlew javadoc` 0 errors/warnings
- `./gradlew checkFormat` 통과
