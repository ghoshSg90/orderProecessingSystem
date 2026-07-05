# Order Processing System

Backend for an e-commerce **Order Processing System** — customers place orders, track their status,
and perform basic order operations; support executives manage all orders. Built with Spring Boot.

---

## Tech stack

| Concern | Choice                                                             |
|---------|--------------------------------------------------------------------|
| Language / build | Java 21, Maven (wrapper included)                                  |
| Framework | Spring Boot 3.5.16 (Web, Data JPA, Validation, Security, Actuator) |
| Database | PostgreSQL                                                         |
| Auth | Stateless JWT (JJWT) + Spring Security, BCrypt passwords           |
| Cache | Caffeine (in-process) — auth lookups                               |
| Tests | JUnit 5 + Mockito                                                  |

---

## Prerequisites

- **JDK 21+**
- **PostgreSQL** running locally on `localhost:5432`
- No local Maven needed — use the bundled wrapper (`./mvnw` / `mvnw.cmd`)

---

## 1. Database setup

Configuration is split across Spring profiles:

| File | Purpose |
|------|---------|
| `application.yaml` | Base config (no secrets); DB **url** + driver, JPA, JWT expiry, `app.log.path`. Defaults the active profile to `dev`. |
| `application-dev.yaml` | **Dev (default)** — DB username/password + JWT secret; verbose logging. |
| `application-qa.yaml` | **QA** — secrets from env vars, `ddl-auto: update`, no seeding. |
| `application-prod.yaml` | **Prod** — all secrets from env vars, `ddl-auto: validate`, no seeding. |
| `application-docker.yaml` | Container demo — env-driven, `create` + seed (used by docker-compose/Helm). |

The **dev** profile connects with:

```
url:      jdbc:postgresql://localhost:5432/orderprocessingsystem_db
username: adminosp
password: password123$
```

Create the database and user to match (or edit `application-dev.yaml` to your own):

```sql
CREATE DATABASE orderprocessingsystem_db;
CREATE USER adminosp WITH PASSWORD 'password123$';
GRANT ALL PRIVILEGES ON DATABASE orderprocessingsystem_db TO adminosp;
```

> **Schema & seed data are automatic (dev).** `ddl-auto: create` rebuilds the schema on every
> startup, and `data.sql` seeds users, addresses, and 50 products each run — so **data resets on
> every restart** (switch to `ddl-auto: update` to persist). In **prod** the schema is only
> validated and seeding is disabled.

### Running with the qa / production profile

```bash
export DB_URL=jdbc:postgresql://<host>:5432/<db>
export DB_USERNAME=<user>
export DB_PASSWORD=<password>
export JWT_SECRET=<base64-256-bit-secret>
./mvnw spring-boot:run -Dspring-boot.run.profiles=prod   # or =qa
```

---

## 2. Run

```bash
./mvnw spring-boot:run
```

The app starts on **http://localhost:8080**. On startup you'll see a log line like:

```
INFO  Application started successfully profile=dev javaVersion=21... database=postgresql cache=Caffeine jwtEnabled=true
```

### Explore the API (Swagger UI)

The app serves under the `/orderProcessingSystem` context path, so once it's up:

| | URL |
|---|-----|
| **Swagger UI** | `http://localhost:8080/orderProcessingSystem/swagger-ui/index.html` |
| **OpenAPI JSON** | `http://localhost:8080/orderProcessingSystem/v3/api-docs` |

To call secured endpoints from Swagger:

1. Log in via `POST /v1/auth/login` (e.g. `supp_admin` / `password123$`) and copy the `token`.
2. Click **Authorize** (padlock, top-right), paste the token, and confirm — no `Bearer ` prefix needed.
3. The token is remembered across page reloads (persist-authorization is enabled).

Swagger UI and `/v3/api-docs` are public; every other endpoint requires the bearer token.

---

## 3. Seed users (credentials)

All three seed users share the password **`password123$`**.

| userId | username | role | Notes |
|--------|----------|------|-------|
| 1 | `supp_admin` | `CUSTOMER_SUPPORT_EXECUTIVE` | Admin endpoints |
| 2 | `sghosh` | `CUSTOMER` | Has addresses 1, 2 |
| 3 | `sghosh2` | `CUSTOMER` | Has addresses 3, 4 |

Products have ids `1..50`; a `CUSTOMER` can browse them via `GET /v1/products`.

---

## 4. Authentication flow (read this before calling secured endpoints)

Every endpoint except `/v1/auth/**` requires a **JWT**. The flow is:

1. **Log in** to get a token:

   As a **customer** (`sghosh`):
   ```bash
   curl -s -X POST http://localhost:8080/orderProcessingSystem/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"userName":"sghosh","password":"password123$"}'
   ```

   As the **support admin** (`supp_admin`) — needed for the admin routes under `/v1/admin/**`:
   ```bash
   curl -s -X POST http://localhost:8080/orderProcessingSystem/v1/auth/login \
     -H "Content-Type: application/json" \
     -d '{"userName":"supp_admin","password":"password123$"}'
   ```

   Response:
   ```json
   { "token": "eyJ...", "userId": 1, "userName": "supp_admin", "userRoleCategory": "CUSTOMER_SUPPORT_EXECUTIVE" }
   ```

2. **Send the token** on subsequent requests:

   ```
   Authorization: Bearer <token>
   ```

Rules:
- A `CUSTOMER` may only access their **own** `/v1/users/{userId}/...` resources (the `{userId}` must
  match the token, else `403`).
- Admin routes (`/v1/admin/**`) require the `CUSTOMER_SUPPORT_EXECUTIVE` role.
- Tokens expire after 1 hour. Missing/invalid token → `401`; wrong role → `403`.

New customers can self-register (always created as `CUSTOMER`):

```bash
curl -X POST http://localhost:8080/orderProcessingSystem/v1/auth/register \
  -H "Content-Type: application/json" \
  -d '{"userName":"jdoe","name":"John Doe","email":"jdoe@mail.com","mobileNumber":"9000000001","password":"MyPass@123"}'
```

---

## 5. API reference

### Public
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/auth/register` | Register a new customer |
| POST | `/v1/auth/login` | Authenticate, returns JWT |

### Customer (`ROLE_CUSTOMER`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/v1/users/{userId}/orders` | Create an order with multiple items |
| GET | `/v1/users/{userId}/orders` | List own orders (paginated, 10/page) |
| GET | `/v1/users/{userId}/orders/{orderId}` | Get one order with line items |
| GET | `/v1/users/{userId}/orders/{orderId}/items` | List that order's items (paginated) |
| PUT | `/v1/users/{userId}/orders/{orderId}/cancel` | Cancel an order (only if `PENDING`) |
| GET | `/v1/products` | Search products, optional `?category=` (paginated) |
| PUT | `/v1/users/{userId}/addresses/{addressId}` | Update one of own addresses |
| DELETE | `/v1/users/{userId}` | Delete own account |

### Admin (`ROLE_CUSTOMER_SUPPORT_EXECUTIVE`)
| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/v1/admin/orders` | List all orders, optional `?status=` (paginated) |
| GET | `/v1/admin/orders/{orderId}` | Full info for any order |
| GET | `/v1/admin/orders/{orderId}/{orderDetailsId}` | A single order-detail line |
| PATCH | `/v1/admin/orders/{orderId}/status` | Update an order's status |

Pagination params: `?page=`, `?size=` (default 10), `?sort=`. Valid `status`/`category` values come
from the `OrderStatus` / `ProductCategory` enums; an invalid value returns `400` with the allowed list.

---

## 6. Example: full order flow (as `sghosh`)

```bash
# 1. login
TOKEN=$(curl -s -X POST http://localhost:8080/orderProcessingSystem/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"userName":"sghosh","password":"password123$"}' | grep -o '"token":"[^"]*"' | cut -d'"' -f4)

# 2. create an order (shipping to address 1, two products)
curl -s -X POST http://localhost:8080/orderProcessingSystem/v1/users/2/orders \
  -H "Authorization: Bearer $TOKEN" -H "Content-Type: application/json" \
  -d '{"shippingAddressId":1,"items":[{"productId":1,"quantity":2},{"productId":11,"quantity":1}]}'

# 3. list own orders
curl -s http://localhost:8080/orderProcessingSystem/v1/users/2/orders -H "Authorization: Bearer $TOKEN"

# 4. cancel order 1 (only works while still PENDING)
curl -s -X PUT http://localhost:8080/orderProcessingSystem/v1/users/2/orders/1/cancel -H "Authorization: Bearer $TOKEN"
```

> **Demo tip:** a background job flips `PENDING` orders to `PROCESSING` every 5 minutes, which also
> closes the cancel window — so cancel promptly after creating an order.

---

## 7. Order status & the background job

Statuses: `PENDING → PROCESSING → SHIPPED → DELIVERED` (plus `CANCELLED`).

- A `@Scheduled` job (`OrderStatusScheduler`) runs **every 5 minutes** and moves all `PENDING`
  orders to `PROCESSING`.
- `SHIPPED` / `DELIVERED` transitions are performed by an admin via
  `PATCH /v1/admin/orders/{orderId}/status`.

---

## 8. Tests

```bash
./mvnw test
```

49 tests total, all green. 48 are pure unit/slice tests (service layer via Mockito, a security-filter
test, and an exception-handler web-slice test) that need **no** database.

> **Note:** the suite also includes one `@SpringBootTest` context-load test (`contextLoads`) that boots
> the full application, so **`mvn test` requires PostgreSQL to be running** (do step 1 first). To run
> only the DB-free unit tests, exclude it:
> ```bash
> ./mvnw test -Dtest='!OrderProcessingSystemApplicationTests'
> ```

---

## 9. Logging

Structured, single-line logs with a per-request correlation id (`requestId`, also returned as the
`X-Request-Id` response header). Full convention and event catalogue in **[LOGGING.md](LOGGING.md)**.

### File logging

Console logging is always on. **File logging is opt-in** — set the log directory in
`application.yaml` (or via env):

```yaml
app:
  log:
    path: ${user.home}/log      # e.g. resolves to $HOME/log; must be writable
```

- If `app.log.path` is **blank**, a warning is printed at startup and the app runs with **console
  only** (it does not fail).
- Log file name: **`<env>_<applicationName>_<startTimestamp>.log`** — e.g.
  `dev_orderProcessingSystem_2026-07-05_11-52-30.log` (env = active profile; timestamp = app start).
- The file's level is per environment (console stays verbose):
  - **dev** → all levels (TRACE+)
  - **qa** → INFO / WARN / ERROR
  - **prod** → WARN / ERROR

> The `org.hibernate.orm.jdbc.bind: TRACE` line (dev profile only) logs SQL bind values — never
> enable it in qa/prod (bind values can contain PII); those profiles set it to `WARN`.

---

## 10. Notes & assumptions

- **Extras beyond the core brief:** JWT auth + role-based access, self-registration, pagination,
  Caffeine caching, DB cascade constraints, and structured logging.
- **Referential integrity:** deleting a customer cascades to their addresses, orders, and order
  lines (`ON DELETE CASCADE`). A customer can only self-delete if all their orders are `CANCELLED`
  or `DELIVERED`.
- **Products** are seed-only (no catalog-management API). Support-executive accounts are seed-only
  (self-registration always creates a `CUSTOMER`).
