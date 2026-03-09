# a2a-spring-boot-samples

Spring Boot samples demonstrating the **Agent-to-Agent (A2A) Protocol** for microagent coordination. An LLM-powered client (using Spring AI + ChatClient) handles natural language queries, intelligently routes them to specialized agents that communicate over the A2A Protocol, and maintains multi-turn conversation context.

## Architecture Overview

```
┌─────────────────────────────────────────────────────────────────┐
│                      User (LLM Chat Interface)                  │
└────────────────────────────┬────────────────────────────────────┘
                             │ Natural Language Query
                             ↓
┌─────────────────────────────────────────────────────────────────┐
│              A2A Client (Port 8080, Spring AI)                  │
│  • ChatClient + Tool-calling for agent routing                 │
│  • Session-based conversation memory (20 message window)        │
│  • Google Gemini 2.5-flash-lite (default LLM)                  │
└────────────────────────────┬────────────────────────────────────┘
                             │ A2A Protocol (JSON-RPC)
                  ┌──────────┴──────────┐
                  ↓                     ↓
    ┌──────────────────────┐  ┌──────────────────────┐
    │ Order Agent (8081)   │  │ Delivery Agent (8082)│
    │ • Order List         │  │ • Track Delivery     │
    │ • Cancel Check       │  │ + Query Order Info   │
    │ (Parallel calls)     │  │                      │
    └──────────┬───────────┘  └──────────┬───────────┘
               │                         │
               ↓ A2A (ROLE_AGENT)        │
         ┌──────────────────┐            │
         │ Payment Agent    │ ←──────────┘
         │ (8083)           │
         │ • Refund Status  │
         │ (Internal only)  │
         └──────────────────┘
```

## Module Structure & Ports

| Module | Port | Responsibility |
|--------|------|----------------|
| **a2a-client** | 8080 | Entry point: Spring AI ChatClient, LLM tool-calling, session-based chat |
| **a2a-order-server** | 8081 | Order management: list, cancellation eligibility check (with parallel delivery/payment calls) |
| **a2a-delivery-server** | 8082 | Shipping: delivery tracking, order info enrichment |
| **a2a-payment-server** | 8083 | Payment/refund status (internal agent-to-agent calls only) |

## Key Communication Patterns

### A2A Protocol (Agent-to-Agent)

Agents communicate exclusively via **A2A Protocol** (JSON-RPC over HTTP):

- **External calls** (Client → Agent): `Message.Role.ROLE_USER` with natural language
- **Internal calls** (Agent → Agent): `Message.Role.ROLE_AGENT` with identifier (e.g., `TRACK-1001`, `ORD-1001`)
  - Server reads `role` from JSON body to set `isInternalCall` boolean
  - Different responses based on call type

**Supported JSON-RPC methods** (`POST /a2a`):

| Method | Description |
|--------|-------------|
| `message/send` | Send a message; returns a task or message event |
| `tasks/get` | Retrieve task by ID |
| `tasks/cancel` | Cancel a task |
| `tasks/list` | List tasks |
| `tasks/pushNotification/create` | Register push notification config for a task |
| `tasks/pushNotification/get` | Get push notification config |
| `tasks/pushNotification/delete` | Delete push notification config |

Streaming requests and unknown types return an `UnsupportedOperationError`. JSON-RPC error responses use HTTP 500.

### Parallel Agent Coordination (Order Cancellation Check)

When checking order cancellation eligibility, Order Agent initiates **concurrent** calls:

1. **Delivery Agent** (with tracking number)
   - Returns delivery status (`배송중`, `배송완료`, etc.)
   - ✗ Blocks cancellation if shipping in progress or completed

2. **Payment Agent** (with order number)
   - Returns refund eligibility (`refundEligible:true/false`)
   - ✗ Blocks cancellation if refund not allowed

Order Agent waits for both responses (timeout: `a2a.client.timeout-seconds`, default 15s) and combines results to determine final cancellation eligibility.

## Session-Based Chat API

The client provides a stateful chat interface with conversation memory.

### Endpoint: `POST /chat`

```json
Request:
{
  "message": "ORD-1001 취소 가능해?",
  "sessionId": "optional-for-continuation",
  "memberId": "optional-user-context"
}

Response:
{
  "sessionId": "session-uuid",
  "response": "주문 ORD-1001은 취소 가능합니다..."
}
```

Same `sessionId` maintains multi-turn conversation context (up to 20 messages per session).

## Quick Start

### 1. Set Environment Variables

```bash
export GOOGLE_API_KEY="your-google-ai-api-key"  # Required for client
# Optional:
export APP_CHAT_PROVIDER="google-genai"  # default
export GOOGLE_GENAI_MODEL="gemini-2.5-flash-lite"  # default
```

### 2. Run All Servers (in separate terminals)

```bash
# Terminal 1: Order Agent
./gradlew :a2a-server:a2a-order-server:bootRun

# Terminal 2: Delivery Agent
./gradlew :a2a-server:a2a-delivery-server:bootRun

# Terminal 3: Payment Agent
./gradlew :a2a-server:a2a-payment-server:bootRun

# Terminal 4: A2A Client (after other servers are ready)
./gradlew :a2a-client:bootRun
```

### 3. Chat Examples

```bash
# New session - Ask about order cancellation
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "ORD-1001 취소 가능해?",
    "memberId": "user123"
  }'

# Continuing same session - Track delivery
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "TRACK-1001 배송 어디쯤이야?",
    "sessionId": "<sessionId-from-previous-response>"
  }'

# Freeform query - Let LLM decide
curl -X POST http://localhost:8080/chat \
  -H "Content-Type: application/json" \
  -d '{
    "message": "주문 상태 확인하고 취소할 수 있으면 취소해줄 수 있어?"
  }'
```

## Build & Test

```bash
# Build all modules
./gradlew build

# Run tests
./gradlew test

# Test specific module
./gradlew :a2a-client:test

# Validate JavaDoc (included in build)
./gradlew javadoc
```

## Documentation

All modules include comprehensive **English JavaDoc documentation**:
- Class-level documentation explaining purpose and role
- Method documentation with `@param` and `@return` tags
- Record documentation with parameter descriptions
- Interface method documentation

View generated docs after build: `build/docs/javadoc/index.html` in each module.

## Technology Stack

- **Java 17**, **Spring Boot 3.3.5**, **Gradle**
- **Spring AI 1.1.2** — LLM integration via ChatClient (Google Gemini by default)
- **A2A Java SDK 1.0.0.Alpha3** — Agent-to-Agent protocol (JSON-RPC)
- **Gson 2.13.2** — JSON parsing
- **Lombok** — boilerplate reduction
