# Mealio Event Driven Design

## Document Information

| Item | Value |
|------|---------|
| Project | Mealio |
| Version | 1.0.0 |
| Status | Approved |
| Scope | Internal Event Communication |

---

# 1. Purpose

This document defines asynchronous communication between Mealio microservices.

Mealio uses:

- REST for external APIs
- gRPC for synchronous internal calls
- Kafka for asynchronous event processing

---

# 2. Why Event Driven?

Goals:

- Loose coupling
- Better scalability
- Background processing
- Event replay
- Independent service evolution

---

# 3. Communication Strategy

| Type | Technology |
|------------|-------------|
| External | REST |
| Internal Sync | gRPC |
| Internal Async | Kafka |

---

# 4. High Level Flow

User

↓

REST

↓

Gateway

↓

Order Service

↓

gRPC

↓

Inventory Service

↓

Order Created Event

↓

Kafka

↓

AI Service

Notification Service

Analytics Service

---

# 5. Event Ownership

Golden Rule:

Only the owning service can publish events about its own data.

Example:

Order Service

publishes

OrderCreatedEvent

Inventory Service

publishes

InventoryUpdatedEvent

User Service

publishes

UserProfileUpdatedEvent

---

# 6. Event Topics

## order.created.v1

Published by:

Order Service

Consumers:

- AI Service
- Notification Service
- Analytics Service

---

## inventory.updated.v1

Published by:

Inventory Service

Consumers:

- Menu Service
- AI Service

---

## user.profile.updated.v1

Published by:

User Service

Consumers:

- AI Service

---

# 7. Event Schema

Example:

OrderCreatedEvent

```json
{
  "eventId": "uuid",
  "correlationId": "uuid",
  "eventType": "ORDER_CREATED",
  "timestamp": "2026-06-08T10:00:00Z",
  "payload": {
    "orderId": "uuid",
    "userId": "uuid",
    "totalPrice": 15.50
  }
}
```

---

# 8. Correlation ID

Every event must contain:

correlationId

Purpose:

- Distributed tracing
- Debugging
- AI audit trail

---

# 9. Event Versioning

Topic naming:

order.created.v1

inventory.updated.v1

Never modify existing payloads.

Breaking changes require:

order.created.v2

---

# 10. Delivery Guarantee

Mealio uses:

At-Least-Once Delivery

Consumers must be idempotent.

Example:

AI Service should ignore duplicated OrderCreated events.

---

# 11. Event Consumption

Consumer Flow:

Kafka

↓

Deserialize

↓

Validate

↓

Business Logic

↓

Commit Offset

---

# 12. AI Integration

AI Service subscribes to:

- order.created.v1
- inventory.updated.v1
- user.profile.updated.v1

Examples:

User updates allergy

↓

User Service

↓

Kafka

↓

AI rebuilds memory embedding

---

# 13. Future Events

payment.completed.v1

meal.saved.v1

recommendation.generated.v1

inventory.low-stock.v1

---

# 14. Development Rules

- Event owner publishes
- Consumers never modify event
- Events immutable
- Correlation ID mandatory
- Idempotent consumers
- Versioned topics
- No synchronous waiting on Kafka