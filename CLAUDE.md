# CLAUDE.md

Guidance for working with the A2A Spring Boot Samples project. This document describes the architecture, key patterns, SDK usage, and best practices.

## Project Overview

A multi-module Spring Boot application demonstrating **Agent-to-Agent (A2A) Protocol** for coordinating specialized microagents. The LLM-powered client uses Spring AI's `ChatClient` with tool-calling to understand natural language queries, route them to appropriate agents, and maintain multi-turn conversation context.

**Current Status (as of March 2026):**
- ✅ All modules have comprehensive English JavaDoc (class, method, and record level)
- ✅ Session-based chat API with conversation memory (20 message window)
- ✅ Spring AI ChatClient integration with Google Gemini 2.5-flash-lite
- ✅ Parallel agent coordination (concurrent Delivery + Payment calls)
- ✅ gradle javadoc validation passes (no errors/warnings)
- ✅ checkFormat validation passes (Spring Java Format compliant)
- ✅ Push notification config methods supported (`tasks/pushNotification/create|get|delete`)
- ✅ Typed JSON-RPC error classes (replaces `A2AErrorCodes` constants)
- ✅ Protobuf-based response serialization via `JSONRPCUtils` + `ProtoUtils`
- ✅ Skill ID-based routing via message metadata (`A2aMetadataKeys.SKILL_ID`); `canHandle()` removed
- ✅ `Message.Role`-based access control in `SkillExecutor` (`requiredRole()`); `boolean isInternalCall` removed

## Module Structure & Ports

| Module | Port | Responsibility |
|--------|------|----------------|
| `a2a-client` | 8080 | Entry point: ChatClient + tool-calling, session management, LLM routing |
| `a2a-order-server` | 8081 | Order queries, cancellation eligibility (with parallel agent calls) |
| `a2a-delivery-server` | 8082 | Shipping tracking, order info enrichment (internal A2A calls) |
| `a2a-payment-server` | 8083 | Refund eligibility checks (internal agent-to-agent only) |

## Architecture & Communication Flow

```
User (Natural Language)
        ↓
A2A Client (8080) — ChatClient + @Tool annotations
  • Parses intent via LLM tool-calling
  • Maintains session memory (20 messages)
  • Routes to agents via A2A Protocol
        ↓ (JSON-RPC)
    ┌─────────┬─────────┐
    ↓         ↓         ↓
Order Agent   Delivery  Payment
(8081)        Agent     Agent
    ↓         (8082)    (8083)
    ├────────→ ├─────→ ↓ (internal calls)
    │         (parallel)
    └─────────────────────
```

### A2A Protocol Details

- **Client → Agent:** `POST /a2a` with `Message.Role.ROLE_USER` + skill ID in `message.metadata()`
- **Agent → Agent:** `POST /a2a` with `Message.Role.ROLE_AGENT` + skill ID in `message.metadata()`
 - Skill ID is stored under `A2aMetadataKeys.SKILL_ID` (`"skillId"`) in message metadata
 - Each `SkillExecutor` declares `skillId()` and `requiredRole()`; `AgentExecutor` routes by skill ID and enforces role

**Supported JSON-RPC methods (non-streaming):**

| Method | Request Type | Description |
|--------|-------------|-------------|
| `message/send` | `SendMessageRequest` | Send a message and receive a task or message event |
| `tasks/get` | `GetTaskRequest` | Retrieve task by ID |
| `tasks/cancel` | `CancelTaskRequest` | Cancel a task |
| `tasks/list` | `ListTasksRequest` | List tasks |
| `tasks/pushNotification/create` | `CreateTaskPushNotificationConfigRequest` | Register push notification config |
| `tasks/pushNotification/get` | `GetTaskPushNotificationConfigRequest` | Get push notification config |
| `tasks/pushNotification/delete` | `DeleteTaskPushNotificationConfigRequest` | Delete push notification config |

### Key Code Patterns

**1. SkillExecutor Interface** (all agents)

```java
public interface SkillExecutor {
  String skillId();           // Skill ID to match against message metadata
  Message.Role requiredRole(); // Caller role required to invoke this skill
  String execute(String message);
}
```

Each agent has one `SkillExecutor` per skill. `AgentExecutor` reads `skillId` from `message.metadata()` via `A2aMetadataKeys.SKILL_ID`, finds the matching executor, and enforces `requiredRole()` before calling `execute()`. No `canHandle()` needed — routing is declarative.

**2. A2A Client Pattern** (agents calling other agents)

```java
// Agent-to-agent communication
@Component
public class A2a*AgentClient {
  private final AtomicReference<AgentCard> agentCardRef = new AtomicReference<>();  // Lazy-loaded, cached

  public Response getInfo(String identifier) {
    Message msg = A2A.toAgentMessage(identifier);  // Use SDK utility
    AgentCard card = resolveAgentCard();
    // Build client, send message, parse artifact via Consumer callback
  }

  private AgentCard resolveAgentCard() {
    AgentCard card = agentCardRef.get();
    if (card == null) {
      synchronized (this) {
        card = agentCardRef.get();
        if (card == null) {
          A2AHttpClient httpClient = A2AHttpClientFactory.create();
          card = new A2ACardResolver(httpClient, agentUrl, null).getAgentCard();
          agentCardRef.set(card);
        }
      }
    }
    return card;
  }
}
```

Key points:
- Build messages with `Message.builder().role(...).parts(...).metadata(Map.of(A2aMetadataKeys.SKILL_ID, skillId)).build()`
- Do **not** use `A2A.toAgentMessage(text)` — it does not carry skill ID metadata
- Cache `AgentCard` via `AtomicReference<AgentCard>` with synchronized double-check block
- Always specify output mode: `List.of("text")`
- Use Consumer callbacks (`BiConsumer<ClientEvent, AgentCard>`) to receive async `TaskEvent` results

**3. A2AJsonRpcController Pattern** (`a2a-spring-boot-server`)

The shared controller uses pattern matching for dispatch and Protobuf for serialization:

```java
// Dispatch: NonStreamingJSONRPCRequest subtype check (no string comparison)
private A2AResponse<?> processNonStreamingRequest(NonStreamingJSONRPCRequest<?> request,
        ServerCallContext context) {
    if (request instanceof SendMessageRequest req) { ... }
    if (request instanceof GetTaskRequest req) { ... }
    if (request instanceof CreateTaskPushNotificationConfigRequest req) { ... }
    // ...
    return generateErrorResponse(request, new UnsupportedOperationError());
}

// Serialization: domain response → Protobuf → JSON-RPC string
private static String serializeResponse(A2AResponse<?> response) {
    if (response instanceof A2AErrorResponse error) {
        return JSONRPCUtils.toJsonRPCErrorResponse(error.getId(), error.getError());
    }
    com.google.protobuf.MessageOrBuilder proto = convertToProto(response);
    return JSONRPCUtils.toJsonRPCResultResponse(response.getId(), proto);
}

// Error mapping: typed error classes, not A2AErrorCodes constants
catch (InvalidParamsJsonMappingException e) {
    error = new A2AErrorResponse(e.getId(), new InvalidParamsError(null, e.getMessage(), null));
}
catch (JsonSyntaxException | JsonProcessingException e) {
    error = new A2AErrorResponse(new JSONParseError(e.getMessage()));
}
```

Key points:
- Parse with `JSONRPCUtils.parseRequestBody(body, null)`, then check `instanceof NonStreamingJSONRPCRequest`
- Each error type maps to its own JSON-RPC error class (not raw codes)
- Error responses use `ResponseEntity.internalServerError()` (HTTP 500), not HTTP 200
- Result responses use `ResponseEntity.ok()` with Protobuf-serialized body

**4. In-Memory Database Pattern**

```java
public class *Database {
  private static final Map<String, Info> DATA = Map.of(
    "ORD-1001", new OrderInfo(...)
  );

  public static Optional<Info> findById(String id) {
    return Optional.ofNullable(DATA.get(id));
  }

  public record Info(...) {}  // Use records for data objects
}
```

**4. Spring AI ChatClient Pattern** (client only)

Tools are defined with `@Tool` annotation, registered at ChatClient build time via `defaultTools(tools.toArray())`:

```java
// Tool definition — Spring AI @Tool annotation
@Component
public class OrderTool extends A2aTool {
  @Tool(description = "주문 취소 가능 여부 확인...")
  public String checkOrderCancellability(OrderCancellabilityRequest request) {
    return sendRequest("order_cancellability_check", request.orderNumber() + " 취소 가능 여부 확인");
  }
}

// ChatClient configuration
@Bean
public <T extends A2aTool> ChatClient chatClient(ChatModel chatModel, ChatMemory chatMemory, List<T> tools) {
  return ChatClient.builder(chatModel)
    .defaultSystem(SYSTEM_PROMPT)
    .defaultTools(tools.toArray())   // Register @Tool-annotated beans
    .defaultAdvisors(MessageChatMemoryAdvisor.builder(chatMemory).build())
    .build();
}

// Orchestration — ChatClient handles tool invocation loop automatically
@Service
public class ChatOrchestrator {
  private final ChatClient chatClient;

  public ChatResponse handle(ChatRequest request) {
    String content = chatClient.prompt()
      .advisors(a -> a.param(ChatMemory.CONVERSATION_ID, request.sessionId()))
      .user(prepareUserMessage(request))
      .call()
      .content();
    return new ChatResponse(request.sessionId(), content);
  }
}
```

- Tools use `@Tool` annotation (not `FunctionToolCallback.builder()`)
- Tool-calling loop is automatic (`internalToolExecutionEnabled=true` by default)
- Session memory is managed via `ChatMemory.CONVERSATION_ID` advisor parameter
- `A2aTool.sendRequest(skillId, text)` builds a message with `A2aMetadataKeys.SKILL_ID` in metadata, then sends via `A2aTransport`

### Parallel Agent Coordination Example

**OrderCancellabilitySkillExecutor** (see full JavaDoc in code):

```java
@Override
public String execute(String message) {
  // Initiate parallel calls
  CompletableFuture<PaymentStatus> paymentFuture =
    CompletableFuture.supplyAsync(() -> paymentClient.getStatus(orderNumber));
  CompletableFuture<DeliveryStatus> deliveryFuture =
    CompletableFuture.supplyAsync(() -> deliveryClient.getStatus(trackingNumber));

  // Wait for both (with timeout)
  PaymentStatus ps = paymentFuture.get(timeoutSeconds, TimeUnit.SECONDS);
  DeliveryStatus ds = deliveryFuture.get(timeoutSeconds, TimeUnit.SECONDS);

  // Combine results to determine cancellability
  boolean cancellable = ps.refundEligible() && !isShipping(ds);
  return formatResponse(cancellable);
}
```

## Documentation Standards

**All public classes, methods, records, and interfaces now have comprehensive English JavaDoc:**

- **Classes:** Purpose, role in system, any special behavior
- **Methods:** What it does, parameters, return values, exceptions
- **Records:** All parameter descriptions
- **Interfaces:** Contract and all method semantics

Example:
```java
/**
 * Skill executor for delivery tracking queries.
 * Handles the {@code track_delivery} skill. Only accessible to external user calls (ROLE_USER).
 */
@Component
public class DeliveryTrackingSkillExecutor implements SkillExecutor {
  @Override public String skillId() { return "track_delivery"; }
  @Override public Message.Role requiredRole() { return Message.Role.ROLE_USER; }

  /**
   * @param message the message text containing a tracking number (TRACK-xxx)
   * @return a formatted delivery status response, optionally enriched with order info
   */
  @Override
  public String execute(String message) { ... }
}
```

Validation: `./gradlew javadoc` passes with 0 errors/warnings.

## Configuration & Environment

### application.yml (Client)

```yaml
app:
  chat:
    provider: ${APP_CHAT_PROVIDER:google-genai}
spring:
  ai:
    google:
      genai:
        api-key: ${GOOGLE_API_KEY}
        chat:
          options:
            model: ${GOOGLE_GENAI_MODEL:gemini-2.5-flash-lite}
            temperature: 0.7
a2a:
  client:
    timeout-seconds: 15
    order-agent-url: http://localhost:8081
    delivery-agent-url: http://localhost:8082
  # etc.
```

### Required Environment Variables

- `GOOGLE_API_KEY` — API key for Google AI (Gemini). Get free tier at [ai.google.dev](https://ai.google.dev)
- Optional: `APP_CHAT_PROVIDER`, `GOOGLE_GENAI_MODEL`

To add OpenAI or other providers:
1. Create new `@Configuration` class with `@ConditionalOnProperty`
2. Build ChatModel and ChatClient beans
3. Set `APP_CHAT_PROVIDER=openai` (or similar)

## Build, Test & Code Quality

```bash
# Full build (includes javadoc validation)
./gradlew build

# JavaDoc validation (0 errors/warnings expected)
./gradlew javadoc

# Code format validation (Spring Java Format)
./gradlew checkFormat

# Auto-format code (if checkFormat fails)
./gradlew format

# Run all tests
./gradlew test

# Run specific module
./gradlew :a2a-client:test
```

### Code Format Standards

This project uses **Spring Java Format** plugin (`io.spring.javaformat 0.0.47`) for consistent code style. All contributions must pass:

```bash
./gradlew checkFormat
```

If format issues are found, auto-fix them:

```bash
./gradlew format
```

Common style enforcements:
- 4-space indentation
- Unix line endings (LF)
- No trailing whitespace
- Consistent brace placement
- Proper import organization

## Session-Based Chat API

### Endpoint: POST /chat

Request/Response use `ChatRequest` and `ChatResponse` records:
- `message` (required): User query
- `sessionId` (optional): Provide to continue existing conversation
- `memberId` (optional): User context (for order queries)

Response includes:
- `response`: LLM-generated reply
- `sessionId`: Session identifier (reuse for follow-up messages)

### Multi-Turn Conversation

Memory window: 20 most recent messages per session.

```bash
# Query 1: New session (no sessionId)
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"ORD-1001 취소 가능해?","memberId":"user1"}'
# Response includes sessionId

# Query 2: Continue (same sessionId)
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{"message":"그럼 배송은?","sessionId":"<from-response>"}'
```

## Technology Stack

- **Java 17**, **Spring Boot 3.3.5**, **Gradle 9.3**
- **Spring AI 1.1.2** — ChatClient (Google Gemini integration)
- **A2A Java SDK 1.0.0.Alpha3** — A2A Protocol (JSON-RPC)
- **Gson 2.13.2** — JSON parsing
- **Lombok** — Boilerplate reduction

## Best Practices (Established in This Project)

1. **Skill ID Routing:** All A2A messages carry `skillId` in `message.metadata()` via `A2aMetadataKeys.SKILL_ID`. `AgentExecutor` routes by skill ID — no `canHandle()` or message content inspection
2. **Role-Based Access Control:** Each `SkillExecutor` declares `requiredRole()`. `AgentExecutor` enforces it before calling `execute()`. No `boolean isInternalCall` passed through layers
3. **Behavior-Based Executor Naming:** Class names reflect what the executor does, not where it's called from (e.g., `DeliveryTrackingSkillExecutor`, not `DeliveryExternalSkillExecutor`)
4. **A2A Message Building:** Use `Message.builder().role(...).parts(...).metadata(Map.of(A2aMetadataKeys.SKILL_ID, skillId)).build()`. Do **not** use `A2A.toAgentMessage()` — it omits skill ID metadata
5. **Agent Card Caching:** Resolve agent cards lazily, cache via `AtomicReference<AgentCard>` with synchronized double-check block
6. **Timeout Centralization:** All A2A client calls use `a2a.client.timeout-seconds` property
7. **JavaDoc Standard:** All public APIs have English documentation (class, method, record level). Validate with `./gradlew javadoc`
8. **Code Format Compliance:** All code must pass `./gradlew checkFormat` (Spring Java Format). Run `./gradlew format` to auto-fix
9. **Internal Response Format:** Agents respond with structured key:value lines for ROLE_AGENT calls (e.g., `status:배송중`, `refundEligible:true`)
10. **Error Handling:** Use typed A2A error classes (`InvalidParamsError`, `MethodNotFoundError`, `InvalidRequestError`, `JSONParseError`, `InternalError`) — not raw `A2AErrorCodes` constants. JSON-RPC error responses return HTTP 500 (`internalServerError()`), not HTTP 200. Wrap skill executor calls in try/catch, return `TaskStatus(TASK_STATE_FAILED)` on exception.
11. **Session Memory:** Use ChatMemory advisor pattern for multi-turn context (not manual state)
