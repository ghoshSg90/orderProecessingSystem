# Features

A complete inventory of what the Order Processing System implements. See **[README.md](README.md)**
for how to run it and **[LOGGING.md](LOGGING.md)** for the logging convention.

---

## 1. Core order features (the assignment)
- **Create order** with multiple items — validates stock, decrements inventory, computes subtotals/total.
- **Retrieve order by ID** — customer and admin views, including line items.
- **List orders** — a customer's own (paginated) and admin all-orders with optional `?status=` filter.
- **Order statuses** `PENDING → PROCESSING → SHIPPED → DELIVERED` (plus `CANCELLED`).
- **Enforced status transitions** — admin status changes are validated against a transition table
  (`PENDING → PROCESSING|CANCELLED`, `PROCESSING → SHIPPED`, `SHIPPED → DELIVERED|PROCESSING`;
  `DELIVERED` and `CANCELLED` are terminal). Illegal moves return a **400** with a state-specific
  message (e.g. "can only be moved to shipped").
- **Cancel order** — only while `PENDING`; restores stock.
- **Line-item ids** — order item responses include `orderDetailsId` for per-line admin lookups.
- **Background job** — auto-advances `PENDING` → `PROCESSING` on a schedule.

## 2. Authentication & security
- **Registration** (`POST /v1/auth/register`) — self-service, always creates a `CUSTOMER`, BCrypt-hashed
  password, uniqueness checks on username/email/mobile.
- **Login** (`POST /v1/auth/login`) — issues a **JWT** carrying userId + role.
- **Stateless JWT security** — auth filter, `JwtService`, `CustomUserDetailsService`, BCrypt encoder.
- **Role-based access** — `CUSTOMER` vs `CUSTOMER_SUPPORT_EXECUTIVE`.
- **Ownership checks** (`requireSelf`) — a customer can only touch their own resources (403 otherwise).
- **Custom 401/403 handlers** returning consistent JSON at the security-filter layer.
- **Graceful deleted-user token handling** — a valid token for a deleted account yields a clean 401,
  not a 500.

## 3. Account & address management (customer)
- **Delete own account** — guarded (only when all orders are `CANCELLED`/`DELIVERED`); cascades to
  addresses, orders, and order line items.
- **Address CRUD** — add, list, update, delete (delete blocked with a clear 400 if the address is
  referenced by any order).

## 4. Product catalog
- **Search products** (`GET /v1/products`) — optional `?category=` filter, paginated, includes stock.

## 5. Admin (support executive)
- List all orders (status filter, paginated), get any order, get a single order-detail line,
  update order status (**validated against the transition table** — see §1).

## 6. Background jobs
- `OrderStatusScheduler` — moves `PENDING` → `PROCESSING`, logs count + duration, error-safe.

## 7. Caching (Caffeine, in-process)
- **Auth-lookup cache** — `@Cacheable` on `loadUserByUsername` (15-min TTL) + `@CacheEvict` on user
  delete, so authentication doesn't hit the DB on every request.

## 8. Pagination
- Standardized on customer order list, order items, admin order list, and product search
  (default 10/page, `PagedModel` responses).
- `@ParameterObject` on the `Pageable` — Swagger renders `page` / `size` / `sort` as discrete
  fields (no misleading raw-object default).

## 9. API documentation (Swagger / springdoc)
- Grouped controllers via `@Tag`; per-endpoint `@Operation` / `@ApiResponses` / `@Parameter`;
  shared `ErrorResponse` schema.
- **JWT Bearer scheme** with an Authorize button; **persist-authorization** (token survives reloads).
- Swagger UI + `/v3/api-docs` are `permitAll`.

## 10. Health checks
- Differentiated **startup / liveness / readiness** probes (liveness is dependency-free; readiness
  checks the DB; startup checks DB + cache), returning proper **503 on down**.
- **Caffeine health indicator** (custom) — also surfaces under `/actuator/health`.

## 11. Logging (SLF4J + Logback)
- Structured single-line logs with a **per-request correlation id** (MDC `requestId` +
  `X-Request-Id` header).
- **Request logging filter**, **startup/shutdown lifecycle logs**, and business-event logs at the
  agreed levels.
- **Environment-specific file logging** — opt-in via `app.log.path`; filename
  `<env>_<appName>_<startTimestamp>.log`; file levels dev=all / qa=info+ / prod=warn+; console always
  verbose; warns and runs console-only if the path is unset.
- Convention + event catalogue in `LOGGING.md`.

## 12. Configuration & profiles
- Profiles: **`dev`** (default), **`qa`**, **`prod`**, **`docker`** — secrets externalized (JWT + DB
  creds via env in qa/prod/docker; committed dev values for convenience).
- Configurable context path (`/orderProcessingSystem`), OSIV disabled, per-env DDL/seed settings.

## 13. Data integrity & business rules
- JPA cascade deletion (user → addresses + orders → line items) with **explicit ordered deletion** to
  avoid the address↔order foreign-key conflict.
- Terminal-status guard on account deletion; order-reference guard on address deletion; stock guards
  on order creation/cancel.
- Six foreign keys with deliberate cascade / no-action design.
- **N+1 mitigation** — all associations `LAZY`, plus a global Hibernate `default_batch_fetch_size`
  (batches lazy loads into `IN (…)` queries) and targeted `@EntityGraph` fetch plans on the order
  **list** (fetch `user`), **detail** (fetch `user`/`shippingAddress`/items/products), and paginated
  **item** finders.

## 14. Error handling
- Central `GlobalExceptionHandler`: 404 / 400 / 400-validation /
  **400 enum-type-mismatch (with allowed values)** / **400 invalid-sort-property** /
  **400 illegal-status-transition** / 401 / 403 / 409 (DB constraint) / 500 catch-all —
  all logged at appropriate levels.

## 15. Containerization & deployment
- **Dockerfile** (multi-stage, JDK21 → JRE21, non-root, HEALTHCHECK).
- **`.dockerignore`** (excludes target, git, IDE, Zone.Identifier cruft).
- **docker-compose.yaml** — app + Postgres, health-gated startup, self-contained demo.
- **Helm chart** — app Deployment/Service + bundled Postgres (PVC), ConfigMap/Secret, ingress toggle,
  and the three differentiated probes.

## 16. Tests & docs
- **65 unit/slice tests** (Mockito service layer — including the order-status **transition-table**
  rules — a security-filter test, an exception-handler test covering enum-mismatch and
  **invalid-sort** 400s, and a registration-validation test) — all green — plus the
  `@SpringBootTest` context-load test (**66 total**).
- Docs: **README.md** (run/setup/API/auth flow), **LOGGING.md** (logging convention), this file.

---

## Known gaps / not implemented
- No product-catalog **write** APIs (products are seed-only).
- No admin endpoint to create support-executive accounts (seed-only; self-registration is always `CUSTOMER`).
- A JWT stays valid until expiry after self-deletion (only the login-time user lookup is guarded).
- No `@WebMvcTest` controller-slice coverage beyond the exception handler.
- `qa` / `prod` profiles require DB + JWT environment variables to run.
