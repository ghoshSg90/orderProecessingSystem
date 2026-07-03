# Logging Convention

This document describes the logging standard for the Order Processing System. Logging uses
**SLF4J** (facade) over **Logback** (Spring Boot default) — no additional dependencies.

---

## 1. Log levels

The five SLF4J levels are used consistently as follows:

| Level   | Use case |
|---------|----------|
| `TRACE` | Method entry/exit, detailed execution flow. **Off by default** (rarely enabled). |
| `DEBUG` | Repository/pagination details, scheduler step-by-step, per-record processing. |
| `INFO`  | Startup/shutdown, login success, registration, order creation, status changes, scheduler summaries, authorization access. |
| `WARN`  | Validation failures, invalid/expired login, unauthorized/forbidden access, business-rule violations, missing resources. |
| `ERROR` | Unhandled/unexpected exceptions, database failures, scheduler execution failures. |

**Rule of thumb:** `ERROR` means *a human should investigate*. An expected outcome of bad user
input (out of stock, duplicate email, wrong password, cancelling a shipped order) is `WARN`, not `ERROR`.

---

## 2. Format

- **One event = one line.** No multi-line log messages (they break log aggregation/correlation).
- **Structured key=value.** Example:

  ```
  INFO  Order created successfully orderId=1045 userId=21 amount=5420 status=PENDING
  ```

- Console pattern (see `application.yaml`):

  ```
  %d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{requestId}] %logger{36} - %msg%n
  ```

---

## 3. Correlation (requestId)

Every HTTP request is tagged with a short `requestId` so all log lines for that request can be traced.

- `CorrelationIdFilter` (runs first, before the security chain) generates the id and puts it in the
  **MDC** under key `requestId`. It appears on every log line via `[%X{requestId}]`.
- The id is returned to the client in the **`X-Request-Id`** response header — quote it in bug reports.
- Logs produced outside a request (startup, scheduler) simply have an empty `[]` slot.

---

## 4. Never log sensitive data

The following must **never** appear in logs:

- Passwords (plain or hashed)
- JWT tokens
- Credit card / payment data
- Personal data beyond the minimum needed for traceability (userId, username, email are logged
  only where operationally necessary — e.g. registration, failed login)

> Note: `org.hibernate.orm.jdbc.bind: TRACE` in `application.yaml` logs SQL bind parameters and is
> **dev-only** — it can surface emails and password hashes. Disable it for any real deployment.

---

## 5. Event catalogue

Where each event is logged and at what level.

| Area | Event | Level | Location |
|------|-------|-------|----------|
| **Startup/Shutdown** | App ready summary (profile, java, db, cache, jwt) | INFO | `ApplicationLifecycleLogger` |
| | App shutting down | INFO | `ApplicationLifecycleLogger` |
| **Requests** | Incoming request (method, uri, userId, role, ip) | INFO | `RequestLoggingFilter` |
| **Authentication** | Login successful (username, role) | INFO | `AuthService` |
| | Login failed (username, reason) | WARN | `AuthService` |
| | JWT expired (user) | WARN | `JwtService` |
| | Invalid JWT (reason) | WARN | `JwtService` |
| | Unauthorized access — no/invalid auth (401) | WARN | `RestAuthenticationEntryPoint` |
| **Registration** | New customer registered (userId, email) | INFO | `AuthService` |
| | Duplicate registration | WARN | `GlobalExceptionHandler` (business rule) |
| **Order creation** | Creating new order (userId, items) | INFO | `OrderService` |
| | Order created (orderId, userId, amount, status) | INFO | `OrderService` |
| | Out of stock / missing product | WARN | `GlobalExceptionHandler` |
| **Order retrieval** | Fetching order / items | DEBUG | `OrderService`, `AdminOrderService` |
| | Order not found | WARN | `GlobalExceptionHandler` |
| **Status change** | Order status updated (orderId, old, new) | INFO | `AdminOrderService` |
| **Scheduler** | Scheduler started | DEBUG | `OrderStatusScheduler` |
| | Pending orders found (count) | INFO | `AdminOrderService` |
| | Order moved PENDING→PROCESSING (per order) | DEBUG | `AdminOrderService` |
| | Scheduler completed (updated, durationMs) | INFO | `OrderStatusScheduler` |
| | Scheduler execution failed | ERROR | `OrderStatusScheduler` |
| **Cancellation** | Cancel request received (orderId, userId) | INFO | `OrderService` |
| | Order cancelled (orderId) | INFO | `OrderService` |
| | Cancellation rejected (message carries current status) | WARN | `GlobalExceptionHandler` |
| **Validation** | Validation failed (field: reason) | WARN | `GlobalExceptionHandler` |
| **Exceptions** | Business-rule violation | WARN | `GlobalExceptionHandler` |
| | Unexpected exception (+ stack trace) | ERROR | `GlobalExceptionHandler` |
| **DB constraint** | Data integrity violation (+ stack trace) | ERROR | `GlobalExceptionHandler` |
| **Pagination** | Fetching page (page, size, status) | DEBUG | `OrderService`, `AdminOrderService` |
| **Authorization** | Admin accessed order list (adminId) | INFO | `AdminOrderController` |
| | Customer fetched own orders (userId) | INFO | `OrderController` |
| | Access denied — authenticated but forbidden (403) | WARN | `RestAccessDeniedHandler` / `GlobalExceptionHandler` |

---

## 6. Design decisions

- **Failure (WARN) logging is centralized in `GlobalExceptionHandler`.** Most business/validation
  failures throw an exception whose message already carries the context (e.g.
  `"Order cannot be cancelled because it is already SHIPPED"`), so services do not duplicate a WARN.
  Exceptions to this rule:
  - **Failed login** is logged in `AuthService` (to include the attempted username); the handler
    stays silent for `AuthenticationException` to avoid a duplicate line.
  - **Expired/invalid JWT** is logged in `JwtService`.
- **Security-layer 401/403** are handled by `RestAuthenticationEntryPoint` and
  `RestAccessDeniedHandler` because role denials happen inside the security filter chain and never
  reach `@RestControllerAdvice`. Controller-level ownership denials (`requireSelf`) are handled by
  the advice.
- **Database constraint violations are `ERROR`** (project decision), returned to clients as `409`.
- **Cache hit/miss and per-SQL logging are intentionally not app-logged** — cache observability is
  better served by Caffeine stats/metrics, and SQL is controlled via the `org.hibernate.*` logger
  levels rather than manual log statements.

---

## 7. Configuration

`src/main/resources/application.yaml`:

```yaml
logging:
  level:
    root: INFO
    com.test.orderProcessingSystem: DEBUG   # set to TRACE for method entry/exit
    org.hibernate.SQL: DEBUG                 # dev only
    org.hibernate.orm.jdbc.bind: TRACE       # dev only — logs bind values, remove in prod
  pattern:
    console: "%d{yyyy-MM-dd HH:mm:ss.SSS} %-5level [%thread] [%X{requestId}] %logger{36} - %msg%n"
```

**To see method entry/exit (`TRACE`):** set `com.test.orderProcessingSystem: TRACE`.

**For production:** set the application logger to `INFO`, and remove the two `org.hibernate.*`
lines (never log SQL bind parameters in production).
