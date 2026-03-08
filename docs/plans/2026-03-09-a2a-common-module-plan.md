# a2a-common 모듈 도입 구현 계획

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** 중복된 `task.artifacts()` 텍스트 추출 로직을 `a2a-common` 공통 모듈의 `MessageUtil`로 통합한다.

**Architecture:** `a2a-common`을 신규 `java-library` 모듈로 생성하고, 기존 `a2a-spring-boot-server`의 `MessageUtil`을 이곳으로 이동한다. `a2a-client`와 `a2a-spring-boot-server`가 `a2a-common`을 의존하도록 수정하고, 4곳의 중복 코드를 `MessageUtil.extractTextFromTask(task)`로 교체한다.

**Tech Stack:** Java 17, Gradle 9.3, A2A Java SDK 1.0.0.Alpha3 (`io.github.a2asdk:a2a-java-sdk-client`)

---

## Task 1: a2a-common 모듈 뼈대 생성

**Files:**
- Create: `a2a-common/build.gradle`
- Modify: `settings.gradle`

**Step 1: settings.gradle에 모듈 추가**

`settings.gradle`을 열고 `include 'a2a-common'`을 추가한다.

```groovy
rootProject.name = 'a2a-spring-boot-samples'

include 'a2a-common'
include 'a2a-client'
include 'a2a-spring-boot-server'
include 'a2a-server:a2a-order-server'
include 'a2a-server:a2a-delivery-server'
include 'a2a-server:a2a-payment-server'
```

**Step 2: a2a-common/build.gradle 생성**

```groovy
apply plugin: 'java-library'
bootJar.enabled = false
jar.enabled = true

dependencies {
    api 'io.github.a2asdk:a2a-java-sdk-client'
}
```

**Step 3: 빌드 확인**

```bash
./gradlew :a2a-common:build
```

Expected: BUILD SUCCESSFUL

**Step 4: 커밋**

```bash
git add a2a-common/build.gradle settings.gradle
git commit -m "build: a2a-common 모듈 뼈대 추가"
```

---

## Task 2: MessageUtil을 a2a-common으로 이동 + extractTextFromTask 추가

**Files:**
- Create: `a2a-common/src/main/java/com/github/cokelee777/a2a/common/utils/MessageUtil.java`
- Create: `a2a-common/src/test/java/com/github/cokelee777/a2a/common/utils/MessageUtilTest.java`
- Delete: `a2a-spring-boot-server/src/main/java/com/github/cokelee777/a2a/server/utils/MessageUtil.java`

**Step 1: 실패하는 테스트 작성**

`a2a-common/src/test/java/com/github/cokelee777/a2a/common/utils/MessageUtilTest.java`:

```java
package com.github.cokelee777.a2a.common.utils;

import io.a2a.spec.*;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MessageUtilTest {

    @Test
    void extractTextFromMessage_returnsConcatenatedText() {
        Message message = Message.builder()
            .role(Message.Role.ROLE_USER)
            .parts(List.of(new TextPart("hello "), new TextPart("world")))
            .build();

        assertThat(MessageUtil.extractTextFromMessage(message)).isEqualTo("hello world");
    }

    @Test
    void extractTextFromMessage_returnsEmptyWhenNoTextParts() {
        Message message = Message.builder()
            .role(Message.Role.ROLE_USER)
            .parts(List.of())
            .build();

        assertThat(MessageUtil.extractTextFromMessage(message)).isEmpty();
    }

    @Test
    void extractTextFromTask_returnsConcatenatedArtifactText() {
        TextPart part1 = new TextPart("status:DELIVERED");
        TextPart part2 = new TextPart("\ntrackingNumber:TRACK-1001");
        Artifact artifact = new Artifact(null, null, List.of(part1, part2), null, null);
        Task task = Task.builder()
            .id("task-1")
            .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED, null, null))
            .artifacts(List.of(artifact))
            .build();

        assertThat(MessageUtil.extractTextFromTask(task))
            .isEqualTo("status:DELIVERED\ntrackingNumber:TRACK-1001");
    }

    @Test
    void extractTextFromTask_returnsEmptyWhenArtifactsNull() {
        Task task = Task.builder()
            .id("task-1")
            .status(new TaskStatus(TaskState.TASK_STATE_COMPLETED, null, null))
            .build();

        assertThat(MessageUtil.extractTextFromTask(task)).isEmpty();
    }
}
```

**Step 2: 테스트 실패 확인**

```bash
./gradlew :a2a-common:test
```

Expected: FAIL — `MessageUtil` 클래스 없음

**Step 3: MessageUtil 구현 (a2a-common에 신규 생성)**

`a2a-common/src/main/java/com/github/cokelee777/a2a/common/utils/MessageUtil.java`:

```java
package com.github.cokelee777.a2a.common.utils;

import io.a2a.spec.Message;
import io.a2a.spec.Part;
import io.a2a.spec.Task;
import io.a2a.spec.TextPart;

/**
 * Utility class for working with A2A SDK objects.
 *
 * <p>
 * Provides helper methods to extract and process text content from {@link Message} and
 * {@link Task} objects.
 */
public class MessageUtil {

    private MessageUtil() {
    }

    /**
     * Extracts the plain text content from a {@link Message}.
     * @param message the message to extract text from
     * @return the concatenated text of all text parts, or an empty string if none
     */
    public static String extractTextFromMessage(Message message) {
        StringBuilder textBuilder = new StringBuilder();
        for (Part<?> part : message.parts()) {
            if (part instanceof TextPart textPart) {
                textBuilder.append(textPart.text());
            }
        }
        return textBuilder.toString();
    }

    /**
     * Extracts the plain text content from all artifacts of a {@link Task}.
     * @param task the task whose artifacts to extract text from
     * @return the concatenated text of all text parts across all artifacts, or an empty
     * string if there are no artifacts or no text parts
     */
    public static String extractTextFromTask(Task task) {
        if (task.artifacts() == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        task.artifacts().forEach(artifact -> artifact.parts().forEach(part -> {
            if (part instanceof TextPart textPart) {
                sb.append(textPart.text());
            }
        }));
        return sb.toString();
    }

}
```

**Step 4: 테스트 통과 확인**

```bash
./gradlew :a2a-common:test
```

Expected: BUILD SUCCESSFUL, all tests pass

**Step 5: a2a-spring-boot-server의 기존 MessageUtil 삭제**

```bash
rm a2a-spring-boot-server/src/main/java/com/github/cokelee777/a2a/server/utils/MessageUtil.java
```

**Step 6: 커밋**

```bash
git add a2a-common/src/
git rm a2a-spring-boot-server/src/main/java/com/github/cokelee777/a2a/server/utils/MessageUtil.java
git commit -m "feat: a2a-common에 MessageUtil 추가 (extractTextFromTask 포함)"
```

---

## Task 3: a2a-spring-boot-server가 a2a-common에 의존하도록 수정

**Files:**
- Modify: `a2a-spring-boot-server/build.gradle`
- Modify: 기존 `MessageUtil`을 import하던 `a2a-spring-boot-server` 내 파일들

**Step 1: build.gradle에 의존성 추가**

`a2a-spring-boot-server/build.gradle`:

```groovy
apply plugin: 'java-library'
bootJar.enabled = false
jar.enabled = true

dependencies {
    api project(':a2a-common')
    api 'org.springframework.boot:spring-boot-starter-web'
    api 'io.github.a2asdk:a2a-java-sdk-server-common'
    api 'io.github.a2asdk:a2a-java-sdk-jsonrpc-common'
}
```

**Step 2: a2a-spring-boot-server 내 MessageUtil import 수정**

`a2a-spring-boot-server` 안에서 `MessageUtil`을 사용하는 파일을 찾아 import 경로를 수정한다.

```bash
grep -rl "a2a.server.utils.MessageUtil" a2a-spring-boot-server/src/
```

찾은 파일마다 import를 아래로 변경:
- 변경 전: `import com.github.cokelee777.a2a.server.utils.MessageUtil;`
- 변경 후: `import com.github.cokelee777.a2a.common.utils.MessageUtil;`

**Step 3: 빌드 확인**

```bash
./gradlew :a2a-spring-boot-server:build
```

Expected: BUILD SUCCESSFUL

**Step 4: 커밋**

```bash
git add a2a-spring-boot-server/
git commit -m "build: a2a-spring-boot-server가 a2a-common 의존하도록 수정"
```

---

## Task 4: a2a-client가 a2a-common에 의존하도록 수정

**Files:**
- Modify: `a2a-client/build.gradle`

**Step 1: build.gradle에 의존성 추가**

`a2a-client/build.gradle`:

```groovy
dependencies {
    implementation project(':a2a-common')
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-validation'
    implementation 'org.springframework.ai:spring-ai-google-genai'
    implementation 'org.springframework.ai:spring-ai-client-chat'
    implementation 'io.github.a2asdk:a2a-java-sdk-client'
}
```

**Step 2: 빌드 확인**

```bash
./gradlew :a2a-client:build
```

Expected: BUILD SUCCESSFUL

**Step 3: 커밋**

```bash
git add a2a-client/build.gradle
git commit -m "build: a2a-client가 a2a-common 의존하도록 수정"
```

---

## Task 5: 4곳의 중복 코드를 MessageUtil.extractTextFromTask로 교체

**Files:**
- Modify: `a2a-server/a2a-order-server/src/main/java/com/github/cokelee777/orderagentserver/client/A2aPaymentAgentClient.java`
- Modify: `a2a-server/a2a-order-server/src/main/java/com/github/cokelee777/orderagentserver/client/A2aDeliveryAgentClient.java`
- Modify: `a2a-server/a2a-delivery-server/src/main/java/com/github/cokelee777/deliveryagentserver/client/A2aOrderAgentClient.java`
- Modify: `a2a-client/src/main/java/com/github/cokelee777/a2aclient/tools/A2aTool.java`

각 파일에서 아래 블록을:

```java
StringBuilder sb = new StringBuilder();
if (task.artifacts() != null) {
    task.artifacts().forEach(artifact -> artifact.parts().forEach(part -> {
        if (part instanceof TextPart textPart) {
            sb.append(textPart.text());
        }
    }));
}
resultFuture.complete(sb.toString());
```

아래로 교체한다:

```java
resultFuture.complete(MessageUtil.extractTextFromTask(task));
```

각 파일에 `import com.github.cokelee777.a2a.common.utils.MessageUtil;`를 추가하고,
더 이상 사용하지 않는 `TextPart` import가 있다면 제거한다.

**Step 1: 4개 파일 모두 수정 후 전체 빌드**

```bash
./gradlew build
```

Expected: BUILD SUCCESSFUL

**Step 2: 커밋**

```bash
git add a2a-server/ a2a-client/src/
git commit -m "refactor: 중복된 artifact 텍스트 추출 로직을 MessageUtil.extractTextFromTask로 통합"
```

---

## Task 6: 최종 검증

**Step 1: javadoc 검증**

```bash
./gradlew javadoc
```

Expected: BUILD SUCCESSFUL, 0 errors/warnings

**Step 2: 코드 포맷 검증**

```bash
./gradlew checkFormat
```

Expected: BUILD SUCCESSFUL

포맷 오류 발생 시:

```bash
./gradlew format
git add -A
git commit -m "style: 코드 포맷 자동 수정"
```

**Step 3: 전체 테스트**

```bash
./gradlew test
```

Expected: BUILD SUCCESSFUL, all tests pass
