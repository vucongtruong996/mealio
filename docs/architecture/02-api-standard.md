# API & Service Interface Standard

## Document Information

| Item    | Value                                                        |
| ------- | ------------------------------------------------------------ |
| Project | Platform Engineering                                         |
| Version | 2.0.0                                                        |
| Status  | Approved                                                     |
| Scope   | All Services, APIs, AI Agents, MCP Servers, and Integrations |

---

# 1. Purpose

This document defines the interface standards for all platform services.

Objectives:

* Consistent service contracts
* Predictable integrations
* AI-agent compatibility
* Technology independence
* Backward compatibility
* Secure and observable systems
* Simplified maintenance and governance

This standard applies to:

* REST APIs
* gRPC APIs
* Event-driven services
* AI Agents
* MCP Servers
* Internal platform services
* External-facing APIs

---

# 2. Core Principles

## 2.1 Contract First

Every service interface MUST be defined before implementation.

Examples:

* OpenAPI
* AsyncAPI
* Protocol Buffers
* MCP Tool Schemas
* JSON Schema

Service contracts are the source of truth.

---

## 2.2 Stateless Interfaces

Interfaces MUST be stateless.

Every request must contain all information required for processing.

Session state must not be stored inside API instances.

---

## 2.3 Machine-Friendly Design

Interfaces must be designed for:

* Humans
* Applications
* AI Agents
* Automation Platforms

All contracts must be self-describing and schema-driven.

---

## 2.4 Backward Compatibility

Non-breaking changes are preferred.

Examples of non-breaking changes:

* Adding optional fields
* Adding new endpoints
* Adding new event types

Examples of breaking changes:

* Removing fields
* Renaming fields
* Changing field types
* Changing endpoint behavior

Breaking changes require a new major version.

---

# 3. Communication Protocols

## External Communication

Preferred order:

1. REST
2. GraphQL (if justified)
3. MCP Tools (for AI interactions)

---

## Internal Communication

Preferred order:

1. gRPC
2. Events / Messaging
3. REST (when appropriate)

Examples:

```text
Frontend
    ↓
REST API
    ↓
Service
```

```text
Service A
    ↓
gRPC
    ↓
Service B
```

```text
Service A
    ↓
Event Bus
    ↓
Service B
```

---

# 4. Versioning

Every public contract must be versioned.

Examples:

```text
/api/v1/users
```

```text
user.created.v1
```

```text
agent-tool.search.v1
```

Breaking changes require a new version.

---

# 5. Resource and Interface Naming

Use nouns.

Good:

```text
/users
/orders
/invoices
/products
```

Avoid verbs in resource names:

```text
/getUser
/createOrder
/processInvoice
```

Actions belong to:

* HTTP methods
* gRPC methods
* MCP tool definitions

---

# 6. Identifier Standards

Identifiers must be globally unique.

Preferred:

```text
UUIDv7
```

Allowed:

```text
UUID
ULID
Snowflake ID
```

Identifiers must be immutable.

---

# 7. Data Formats

Preferred formats:

```text
JSON
Protocol Buffers
Avro
```

JSON naming convention:

```text
camelCase
```

Example:

```json
{
  "userId": "uuid",
  "firstName": "John",
  "lastName": "Doe"
}
```

---

# 8. Request Contract Rules

Requests should only contain writable fields.

Do not accept:

```text
id
createdAt
updatedAt
deletedAt
```

unless explicitly required.

Requests must be validated before processing.

---

# 9. Response Contract Rules

Standard success response:

```json
{
  "data": {},
  "meta": {
    "requestId": "uuid",
    "timestamp": "2026-06-07T10:00:00Z"
  }
}
```

Collection response:

```json
{
  "data": [],
  "pagination": {
    "page": 0,
    "size": 20,
    "totalItems": 100,
    "totalPages": 5
  }
}
```

Responses must never expose:

* Database entities
* Internal implementation details
* Secrets
* Access tokens

---

# 10. Error Standard

Unified format:

```json
{
  "error": {
    "code": "RESOURCE_NOT_FOUND",
    "message": "Resource not found",
    "details": {}
  },
  "meta": {
    "requestId": "uuid"
  }
}
```

Error codes must be:

```text
UPPER_SNAKE_CASE
```

Examples:

```text
RESOURCE_NOT_FOUND
VALIDATION_ERROR
ACCESS_DENIED
RATE_LIMIT_EXCEEDED
INTERNAL_ERROR
```

---

# 11. Pagination, Filtering and Sorting

Pagination:

```text
?page=0&size=20
```

Filtering:

```text
?status=ACTIVE
```

Sorting:

```text
?sort=name,asc
```

Default page size:

```text
20
```

Maximum page size:

```text
100
```

---

# 12. Standard Status Codes

| Status | Meaning          |
| ------ | ---------------- |
| 200    | Success          |
| 201    | Created          |
| 202    | Accepted         |
| 204    | No Content       |
| 400    | Bad Request      |
| 401    | Unauthorized     |
| 403    | Forbidden        |
| 404    | Not Found        |
| 409    | Conflict         |
| 422    | Validation Error |
| 429    | Rate Limited     |
| 500    | Internal Error   |

---

# 13. Security

Authentication mechanisms may include:

* JWT
* OAuth2
* Service Accounts
* API Keys
* mTLS

All communication must use encrypted transport.

Examples:

```text
HTTPS
TLS
mTLS
```

Services must never trust client-supplied identities.

Identity must be validated through approved authentication mechanisms.

---

# 14. Observability

Every request must support:

```text
X-Request-Id
X-Correlation-Id
```

Services must emit:

* Logs
* Metrics
* Traces

Minimum logging:

* Request ID
* Service Name
* Duration
* Status Code
* Error Code

---

# 15. Documentation

Every interface must provide machine-readable documentation.

Examples:

```text
OpenAPI
AsyncAPI
Proto Files
JSON Schema
MCP Tool Schema
```

Documentation must be versioned together with code.

---

# 16. Idempotency

The following operations must be idempotent:

```text
GET
PUT
DELETE
```

Critical create operations should support:

```text
Idempotency-Key
```

Examples:

* Payments
* Orders
* Billing
* External integrations

---

# 17. Event Standards

Events must be immutable.

Naming convention:

```text
domain.entity.action.version
```

Examples:

```text
user.created.v1
order.completed.v1
payment.failed.v1
```

Events should contain:

```json
{
  "eventId": "uuid",
  "eventType": "user.created.v1",
  "occurredAt": "timestamp",
  "data": {}
}
```

---

# 18. AI Agent Standards

AI Agents must interact through approved interfaces only.

Allowed:

* REST APIs
* gRPC APIs
* MCP Tools
* Event Consumers

Not allowed:

* Direct database access
* Direct infrastructure access
* Bypassing service boundaries

Agent tools must:

* Define input schema
* Define output schema
* Return structured responses
* Be deterministic where possible

---

# 19. MCP Tool Standards

Every MCP tool must define:

* Name
* Description
* Input Schema
* Output Schema

Example:

```json
{
  "name": "createOrder",
  "description": "Create customer order",
  "inputSchema": {},
  "outputSchema": {}
}
```

Tool names must be stable and versioned.

---

# 20. Governance

All new interfaces must pass:

* Architecture Review
* Security Review
* API Review

No public interface may be released without documentation.

---

# 21. Request Tracing

Every request should contain:

`X-Correlation-Id`

If absent, the Gateway generates one.

Example:

```text
X-Correlation-Id: 7d7d4c7d-3f8c-4b8a-9d9e-123456789abc
```

This ID is propagated across REST, gRPC, and Kafka events.

Purpose:
* Distributed tracing
* Debugging
* Audit logs
* AI action tracking

---

# 22. Development Principles

* Contract First
* API First
* Documentation First
* Security By Default
* Observability By Default
* Backward Compatibility
* Stateless Services
* Event-Driven Friendly
* AI Agent Compatible
* Platform Agnostic
* Cloud Native
* Easy To Scale
* Easy To Maintain
