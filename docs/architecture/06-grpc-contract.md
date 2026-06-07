# Mealio gRPC Contract Standard

## 1. Purpose

This document defines internal service-to-service communication.

Mealio uses:

- REST for external clients.
- gRPC for internal services.

---

## 2. Design Goals

- Low latency
- Strong typing
- Contract first
- Language independent
- Easy code generation

---

## 3. Communication Pattern

External:

Client

↓

REST

↓

Gateway

↓

Microservices

Internal:

Service A

↓

gRPC

↓

Service B

---

## 4. Folder Structure

mealio-contracts/

grpc/

menu/

menu.proto

inventory/

inventory.proto

user/

user.proto

order/

order.proto

---

## 5. Proto Convention

syntax = "proto3";

package mealio.menu.v1;

option java_multiple_files = true;

option java_package =
"com.mealio.contracts.menu.v1";

---

## 6. Menu Service Contract

service MenuRpcService {

rpc GetMenuItem(
GetMenuItemRequest
)
returns
(
MenuItemResponse
);

rpc GetMenuItems(
GetMenuItemsRequest
)
returns
(
GetMenuItemsResponse
);

rpc CheckAvailability(
CheckAvailabilityRequest
)
returns
(
CheckAvailabilityResponse
);

}

---

message GetMenuItemRequest {

string id = 1;

}

---

message MenuItemResponse {

string id = 1;

string name = 2;

double price = 3;

int32 calories = 4;

bool available = 5;

}

---

## 7. Inventory Service Contract

service InventoryRpcService {

rpc CheckStock(
CheckStockRequest
)
returns
(
CheckStockResponse
);

}

---

message CheckStockRequest {

string ingredientId = 1;

double quantity = 2;

}

---

message CheckStockResponse {

bool available = 1;

double remainingQuantity = 2;

}

---

## 8. User Service Contract

service UserRpcService {

rpc GetUserProfile(
GetUserProfileRequest
)
returns
(
UserProfileResponse
);

}

---

message UserProfileResponse {

string userId = 1;

repeated string allergies = 2;

string nutritionGoal = 3;

}

---

## 9. Order Service Contract

Order Service should expose only read operations.

Example:

rpc GetOrder(
GetOrderRequest
)
returns
(
OrderResponse
);

---

## 10. UUID Rule

All IDs use string.

Example:

550e8400-e29b-41d4-a716-446655440000

Proto does not have UUID type.

---

## 11. Versioning

Never modify existing contracts.

mealio.menu.v1

mealio.menu.v2

---

## 12. Error Mapping

OK

INVALID_ARGUMENT

NOT_FOUND

ALREADY_EXISTS

FAILED_PRECONDITION

INTERNAL

---

## 13. AI Agent Integration

AI

↓

MCP Tool

↓

gRPC Client

↓

Microservice

↓

Database

AI never accesses repositories.

---

## 14. Development Rules

- Proto first
- Backward compatible
- DTO != Entity
- UUID as string
- Read operations preferred
- No database leakage