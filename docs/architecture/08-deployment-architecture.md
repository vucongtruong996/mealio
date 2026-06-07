# Mealio Deployment Architecture

## Document Information

| Item | Value |
|------|---------|
| Project | Mealio |
| Version | 1.0.0 |
| Status | Approved |
| Scope | Infrastructure & Deployment |

---

# 1. Purpose

This document defines how Mealio is deployed across local and cloud environments.

Goals:

- Easy local development
- Low operational overhead
- Horizontal scalability
- Cloud-native architecture
- Future Kubernetes readiness

---

# 2. Deployment Evolution

## Phase 1

Local Development

```text
Docker Compose

├── postgres
├── redis
├── menu-service
├── user-service
├── order-service
├── inventory-service
└── ai-service
```

---

## Phase 2

AWS ECS Fargate

```text
Internet

↓

Application Load Balancer

↓

ECS Cluster

├── menu-service
├── user-service
├── order-service
├── inventory-service
└── ai-service

↓

RDS PostgreSQL

↓

ElastiCache Redis
```

---

## Phase 3

Future Kubernetes

```text
Internet

↓

ALB

↓

Amazon EKS

↓

Pods

↓

RDS + Redis
```

---

# 3. Local Development

Developers run the complete platform using Docker Compose.

Services:

| Service |
|----------|
| postgres |
| redis |
| menu-service |
| user-service |
| order-service |
| inventory-service |
| ai-service |

---

# 4. Docker Network

All containers share one private network.

```text
mealio-network

menu-service

order-service

user-service

inventory-service

ai-service

postgres

redis
```

Services communicate using container names.

Example:

```text
jdbc:postgresql://postgres:5432/mealio
```

---

# 5. Database Deployment

Development:

Single PostgreSQL instance.

Schemas:

- menu_schema
- user_schema
- order_schema
- inventory_schema
- ai_schema

Production:

Separate RDS databases may be introduced without code changes.

---

# 6. Redis

Purpose:

- Menu cache
- User preference cache
- Inventory lock
- Future session cache

TTL examples:

| Data | TTL |
|--------|--------|
| Menu | 10 min |
| User Preference | 30 min |
| Inventory Lock | 5 min |

---

# 7. Container Images

Naming convention:

mealio/menu-service

mealio/user-service

mealio/order-service

mealio/inventory-service

mealio/ai-service

Versioning:

1.0.0

1.1.0

2.0.0

Never use latest.

---

# 8. Environment Variables

Example:

```text
DB_HOST=postgres
DB_PORT=5432
DB_NAME=mealio

REDIS_HOST=redis

SPRING_PROFILES_ACTIVE=local
```

Secrets are never committed.

---

# 9. AWS Architecture

```text
Client

↓

Route53

↓

Application Load Balancer

↓

ECS Fargate

├── API Gateway
├── Menu Service
├── User Service
├── Order Service
├── Inventory Service
└── AI Service

↓

Amazon RDS PostgreSQL

↓

Amazon ElastiCache Redis
```

---

# 10. AI Deployment

AI Service runs independently.

Responsibilities:

- LLM integration
- MCP tools
- RAG retrieval
- Recommendation engine

AI failure must not stop ordering.

---

# 11. Logging

Every service writes structured JSON logs.

Fields:

- timestamp
- serviceName
- correlationId
- traceId
- level
- message

Example:

```json
{
  "serviceName":"menu-service",
  "correlationId":"uuid",
  "level":"INFO",
  "message":"Menu item created"
}
```

---

# 12. Observability

Future integrations:

- Micrometer
- Prometheus
- Grafana
- AWS CloudWatch

---

# 13. Distributed Tracing

Every request contains:

X-Correlation-Id

Flow:

Client

↓

Gateway

↓

Menu Service

↓

Inventory Service

↓

AI Service

Same correlation ID propagated.

---

# 14. CI/CD Pipeline

GitHub

↓

Pull Request

↓

Build

↓

Unit Tests

↓

Integration Tests

↓

Docker Build

↓

Push Image

↓

Deploy

---

# 15. Deployment Strategy

Current:

Rolling Update

Future:

Blue-Green Deployment

Canary Deployment

---

# 16. Scaling Strategy

Menu Service:

Horizontal scaling.

Inventory Service:

Horizontal scaling with optimistic locking.

AI Service:

Independent scaling.

Order Service:

Horizontal scaling.

---

# 17. Failure Recovery

If AI Service fails:

- Menu browsing continues.
- Order creation continues.
- Inventory checking continues.

AI features become unavailable.

Graceful degradation is mandatory.

---

# 18. Development Rules

- Docker first
- Environment driven configuration
- Immutable containers
- Never use latest tag
- Correlation ID mandatory
- AI isolated from core business
- Database per service ownership