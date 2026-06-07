# Mealio Database Design

## Document Information

| Item | Value |
|------|---------|
| Project | Mealio |
| Version | 1.0.0 |
| Status | Approved |
| Scope | Database Architecture |

---

# 1. Purpose

This document defines the database architecture for the Mealio platform.

Goals:

- Independent microservices
- Easy scaling
- AI-friendly data access
- Future cloud migration
- Maintainable schemas

---

# 2. Design Principles

## 2.1 Database Per Service

Every microservice owns its own data.

```text
menu-service
      │
      ▼
menu_schema

order-service
      │
      ▼
order_schema

user-service
      │
      ▼
user_schema

inventory-service
      │
      ▼
inventory_schema

ai-service
      │
      ▼
ai_schema
```

---

## Why one PostgreSQL instance?

For local development:

- Easier setup
- Lower memory usage
- Simpler Docker Compose

Future production:

```text
Amazon RDS

├── menu-db
├── order-db
├── user-db
├── inventory-db
└── ai-db
```

No application code changes required.

---

## 2.2 Services Never Share Tables

❌ Forbidden:

```text
OrderService

SELECT * FROM menu_schema.menu_item;
```

✅ Correct:

```text
OrderService

↓

gRPC

↓

MenuService

↓

Database
```

---

## 2.3 AI Never Reads Database Directly

```text
AI

↓

MCP Tool

↓

REST/gRPC

↓

Backend Service

↓

Database
```

---

# 3. Why PostgreSQL?

Reasons:

## ACID Compliance

Orders and payments require consistency.

---

## JSONB Support

Useful for AI metadata.

Example:

```json
{
  "visionModel": "gemini",
  "confidence": 0.96
}
```

---

## pgvector

Allows vector search inside PostgreSQL.

No separate Pinecone or Weaviate required.

---

## Mature Ecosystem

- Spring Boot support
- Testcontainers
- AWS RDS
- Flyway

---

# 4. UUID Strategy

Every primary key uses UUID.

Example:

```
550e8400-e29b-41d4-a716-446655440000
```

Reasons:

- Distributed systems
- No collision
- No sequence bottleneck
- Easier data migration

---

# 5. Common Audit Fields

Every table contains:

| Column |
|----------|
| id |
| created_at |
| updated_at |
| deleted_at |

Example:

```sql
created_at TIMESTAMP NOT NULL,
updated_at TIMESTAMP NOT NULL,
deleted_at TIMESTAMP NULL
```

Soft delete preferred.

---

# 6. Menu Service Schema

## menu_item

| Column | Type |
|----------|----------|
| id | UUID |
| name | VARCHAR |
| description | TEXT |
| category_id | UUID |
| calories | INTEGER |
| protein | DECIMAL |
| carbohydrates | DECIMAL |
| fat | DECIMAL |
| image_url | VARCHAR |
| active | BOOLEAN |
| created_at | TIMESTAMP |
| updated_at | TIMESTAMP |
| deleted_at | TIMESTAMP |

---

## category

| Column |
|----------|
| id |
| name |
| description |

---

# 7. User Service Schema

## user_profile

| Column |
|----------|
| id |
| email |
| full_name |
| age |
| gender |
| height |
| weight |
| goal |
| created_at |
| updated_at |

---

## allergy

| Column |
|----------|
| id |
| user_id |
| allergen |

Examples:

- Peanut
- Milk
- Gluten

---

## preference

| Column |
|----------|
| id |
| user_id |
| preference_key |
| preference_value |

Examples:

```text
favorite_drink=americano

avoid=sugar
```

---

# 8. Order Service Schema

## orders

| Column |
|----------|
| id |
| user_id |
| status |
| total_price |
| created_at |
| updated_at |

---

## order_item

| Column |
|----------|
| id |
| order_id |
| menu_item_id |
| quantity |
| unit_price |

menu_item_id is a reference only.

No foreign key across services.

---

# 9. Inventory Service Schema

## inventory_item

| Column |
|----------|
| id |
| ingredient_name |
| available_quantity |
| unit |
| updated_at |

---

## inventory_lock

Temporary reservation.

| Column |
|----------|
| id |
| order_id |
| expires_at |

Used during checkout.

---

# 10. AI Service Schema

## meal_history

| Column |
|----------|
| id |
| user_id |
| menu_item_id |
| quantity |
| calories |
| consumed_at |

---

## ai_memory

| Column |
|----------|
| id |
| user_id |
| summary |
| embedding |

embedding uses pgvector.

---

## ai_interaction_log

| Column |
|----------|
| id |
| correlation_id |
| user_prompt |
| tool_called |
| ai_response |
| created_at |

Used for debugging.

---

# 11. RAG Storage

Knowledge Sources:

- Nutrition guides
- Menu descriptions
- User summaries
- Meal history summaries

Stored in:

```text
ai_schema.ai_memory
```

Example:

| id | summary | embedding |
|----|----------|-------------|
| uuid | User likes spicy food | [0.23, 0.81, ...] |

---

# 12. Database Migration

Use:

Flyway

Structure:

```text
src/main/resources/db/migration

V1__create_menu_tables.sql

V2__add_category_table.sql

V3__add_nutrition_columns.sql
```

Never modify old migrations.

Always create new versions.

---

# 13. Caching Strategy

Redis used for:

## Menu Cache

TTL: 10 minutes

---

## User Preference Cache

TTL: 30 minutes

---

## Inventory Lock

TTL: 5 minutes

---

# 14. Future Evolution

Current:

```text
PostgreSQL

├── menu_schema
├── order_schema
├── user_schema
├── inventory_schema
└── ai_schema
```

Future:

```text
RDS

├── menu-db
├── order-db
├── user-db
├── inventory-db
└── ai-db
```

No service code changes.

---

# 15. Development Rules

- Database per Service
- No cross-service joins
- UUID everywhere
- Soft delete preferred
- Flyway mandatory
- AI never accesses DB directly
- pgvector for RAG
- Redis for caching