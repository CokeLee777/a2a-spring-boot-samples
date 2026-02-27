# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

A multi-module Spring Boot project demonstrating the **Agent-to-Agent (A2A) Protocol** for coordinating microagents. An LLM-powered client routes natural language queries to specialized agents that communicate with each other over the A2A Protocol (JSON-RPC over HTTP).

## Module Structure & Ports

| Module | Port | Responsibility |
|--------|------|----------------|
| `a2a-client` | 8080 | Entry point: LLM intent analysis + agent routing |
| `a2a-server/a2a-order-server` | 8081 | Order cancellation eligibility check |
| `a2a-server/a2a-delivery-server` | 8082 | Delivery tracking + order enrichment |
| `a2a-server/a2a-payment-server` | 8083 | Payment/refund status (internal only) |

## Build & Run Commands

```bash
# Run all servers (each in separate terminal)
./gradlew :a2a-server:a2a-order-server:bootRun
./gradlew :a2a-server:a2a-delivery-server:bootRun
./gradlew :a2a-server:a2a-payment-server:bootRun
./gradlew :a2a-client:bootRun

# Build
./gradlew build

# Test
./gradlew test

# Test single module
./gradlew :a2a-client:test
```

**Required environment variable:** `OPENAI_API_KEY` (used for LLM routing in the client)

## Architecture

### Communication Flow

```
User → A2A Client (8080) → [LLM intent extraction] → Order Agent (8081)
                                                    → Delivery Agent (8082)
                                                          ↕ ROLE_AGENT
                                                    Payment Agent (8083)
```

- **Client → Agents:** Standard A2A Protocol calls with `Message.Role.ROLE_USER` and natural language messages
- **Agent → Agent:** `Message.Role.ROLE_AGENT` with just the identifier (e.g. `TRACK-xxx`, `ORD-xxx`). Server reads `role` from JSON body to set `isInternalCall` flag.
- **Parallel calls:** Order Agent calls Delivery + Payment Agents concurrently with a configurable timeout (`a2a.client.timeout-seconds`)

### Key Patterns

**Skill Executors** (`*SkillExecutor.java`): Each agent implements `SkillExecutor` to handle incoming A2A requests. Internal vs. external calls are distinguished by the `isInternalCall` boolean (derived from `Message.Role.ROLE_AGENT` in the request body), **not** string prefixes.

**Agent Clients** (`A2a*Client.java`): Typed HTTP clients that wrap A2A Protocol calls to other agents.

**In-memory databases** (`*Database.java`): Static `ConcurrentHashMap` with hardcoded sample data. No persistence layer.

### Sample Data

- Orders: `ORD-1001`, `ORD-2002`, `ORD-3003`
- Tracking numbers: `TRACK-1001`, `TRACK-2002`, `TRACK-3003`
- Payments reference order IDs

### LLM Configuration

The client uses Spring AI with OpenAI-compatible API. Default model is `gemini-2.5-flash-lite` via Google's OpenAI-compatible endpoint. Configure via `application.yml`:

```yaml
spring.ai.openai.chat.base-url: ${OPENAI_BASE_URL:https://generativelanguage.googleapis.com/v1beta/openai/}
spring.ai.openai.chat.options.model: ${OPENAI_MODEL:gemini-2.5-flash-lite}
```

## Technology Stack

- **Java 17**, **Spring Boot 3.3.5**, **Gradle**
- **Spring AI 1.1.2** — LLM integration (OpenAI-compatible)
- **A2A Java SDK 1.0.0.Alpha3** — Agent-to-Agent protocol
- **Gson 2.13.2** — JSON parsing
- **Lombok** — boilerplate reduction

## API Examples

```bash
# Order cancellation check (Korean natural language)
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "ORD-1001 취소 가능해?"}'

# Delivery tracking
curl -X POST http://localhost:8080/api/chat \
  -H "Content-Type: application/json" \
  -d '{"message": "TRACK-1001 배송 어디쯤이야?"}'
```
