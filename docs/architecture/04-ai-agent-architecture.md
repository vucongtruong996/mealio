# Mealio AI Agent Architecture

## Document Information

| Item    | Value      |
| ------- | ---------- |
| Project | Mealio     |
| Version | 1.0.0      |
| Status  | Approved   |
| Scope   | AI Service |

---

# 1. Purpose

This document defines the AI architecture for the Mealio platform.

The goal is to build an AI-Agentic system rather than a traditional chatbot.

The AI layer should:

* Understand user intent
* Plan execution steps
* Invoke backend tools
* Aggregate results
* Generate natural language responses

The AI layer must never become the source of truth for business data.

---

# 2. Design Principles

## 2.1 AI Is Not The Application

Traditional architecture:

```text
Frontend
    ↓
Backend
    ↓
Database
```

Mealio architecture:

```text
Frontend
    ↓
Backend Services
    ↓
Database

        ▲

        │

   AI Orchestrator
```

Backend services remain fully functional even if the AI Service is offline.

---

## 2.2 AI Never Owns Business Logic

Bad:

```text
AI:
Deduct inventory by 2 eggs.
```

Good:

```text
AI

↓

createOrder()

↓

Order Service

↓

Inventory Service
```

Business rules belong to backend services.

---

## 2.3 AI Never Accesses Databases Directly

Forbidden:

```text
AI
   ↓
PostgreSQL
```

Required:

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

# 3. High Level Architecture

```text
                    User

                      │

                Web Frontend

                      │

                 REST API

                      │

              AI Orchestrator

                      │

     ┌────────────────┼────────────────┐

     ▼                ▼                ▼

 Menu Agent     Nutrition Agent   Memory Agent

                      │

                      ▼

                 MCP Tools

     ┌────────────────┼────────────────┐

     ▼                ▼                ▼

 Menu API        User API       Order API

                      │

                      ▼

                Inventory API
```

---

# 4. AI Components

## 4.1 LLM

The Large Language Model provides:

* Natural language understanding
* Planning
* Reasoning
* Response generation

Possible providers:

* Google Gemini
* Ollama
* OpenAI-compatible models

The LLM never performs direct business operations.

---

## 4.2 AI Orchestrator

The orchestrator is the central brain.

Responsibilities:

* Receive user requests
* Select required agents
* Coordinate execution
* Merge results
* Generate final response

Example:

User:

> I had two eggs this morning. Save it and calculate my calories.

Execution:

```text
Orchestrator

↓

Food Recognition Agent

↓

Nutrition Agent

↓

Memory Agent

↓

Generate Response
```

---

# 5. Specialized Agents

## 5.1 Menu Agent

Responsibilities:

* Search menu items
* Find similar dishes
* Create draft menu items
* Analyze uploaded food images

Example:

User:

> I ate an egg this morning.

Agent:

* Searches existing menu
* Creates new draft if missing
* Requests nutritional analysis

---

## 5.2 Nutrition Agent

Responsibilities:

* Calculate calories
* Calculate protein
* Calculate carbohydrates
* Calculate fat
* Verify dietary constraints

Example:

User:

> Recommend a dinner under 600 calories.

---

## 5.3 Memory Agent

Responsibilities:

* Store meal history
* Store preferences
* Store habits
* Retrieve previous interactions

Example:

User:

> Recommend something similar to what I liked last week.

---

## 5.4 Recommendation Agent

Responsibilities:

* Analyze user history
* Analyze allergies
* Analyze goals
* Suggest meals

Inputs:

* User profile
* Meal history
* Current nutrition targets

---

# 6. MCP Architecture

Model Context Protocol (MCP) provides a standard interface between AI and backend systems.

Architecture:

```text
LLM

↓

Tool Selection

↓

MCP Server

↓

Backend API

↓

Database
```

Mealio MCP Tools:

| Tool               | Purpose                 |
| ------------------ | ----------------------- |
| searchMenu         | Search menu catalog     |
| createOrder        | Create new order        |
| saveMealHistory    | Store consumed meal     |
| calculateNutrition | Nutrition analysis      |
| getUserProfile     | Retrieve profile        |
| getMealHistory     | Retrieve previous meals |
| searchInventory    | Verify stock            |

---

# 7. RAG Architecture

RAG is used to enhance reasoning with domain knowledge.

The LLM should not rely solely on its training data.

---

## Knowledge Sources

* Menu catalog
* User meal history
* Nutrition database
* Dietary guidelines
* AI memory summaries

---

## RAG Flow

```text
User Question

↓

Embedding

↓

Vector Search

↓

Relevant Documents

↓

LLM Context

↓

Final Response
```

---

# 8. Memory Model

Mealio uses three memory levels.

## Short Term Memory

Current conversation.

Example:

> User already said they dislike mushrooms.

---

## Long Term Memory

Stored preferences.

Examples:

* Favorite drinks
* Allergies
* Dietary goals

---

## Semantic Memory

Generated summaries.

Example:

```text
User usually prefers high protein breakfasts and avoids dairy.
```

Stored in vector database.

---

# 9. Example Agent Workflow

Scenario:

User:

> I ate two boiled eggs this morning. Save it and update my daily calories.

Execution:

```text
User

↓

AI Orchestrator

↓

Menu Agent
(Search existing food)

↓

Nutrition Agent
(Calculate calories)

↓

Memory Agent
(Store history)

↓

saveMealHistory()

↓

Backend API

↓

Database

↓

LLM Response
```

Response:

> I have recorded two boiled eggs for breakfast. Estimated nutrition: 156 kcal, 13g protein, 11g fat, 1g carbohydrates.

---

# 10. Image Understanding

Future capability:

User uploads:

egg.jpg

Flow:

```text
Image Upload

↓

Media Service

↓

AI Vision Model

↓

Menu Agent

↓

Nutrition Agent

↓

Memory Agent

↓

Store Meal History
```

The AI may ask for confirmation before saving.

---

# 11. AI Safety Rules

1. AI never writes directly to databases.

2. AI never bypasses backend APIs.

3. AI never owns business rules.

4. AI actions must be auditable.

5. Backend services remain the source of truth.

6. Every tool invocation should include a Correlation ID.

---

# 12. Failure Handling

If AI fails:

* Menu browsing still works.
* Order creation still works.
* Inventory still works.

AI features degrade gracefully.

Example:

```text
AI unavailable.

Fallback:
Traditional menu search.
```

---

# 13. Future Multi-Agent Collaboration

Future architecture:

```text
                 Planner Agent

                       │

      ┌────────────────┼────────────────┐

      ▼                ▼                ▼

 Menu Agent    Nutrition Agent   Memory Agent

      │                │                │

      └────────────────┼────────────────┘

                       ▼

                Response Generator
```

Agents work independently and return structured outputs to the planner.

---

# 14. Development Principles

* AI is an orchestration layer.
* Backend owns business logic.
* MCP is the integration boundary.
* RAG provides contextual knowledge.
* Memory improves personalization.
* AI must be explainable.
* AI failures must not break the platform.
