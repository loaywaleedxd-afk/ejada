# Ejada E-Commerce — Spring Cloud Microservices

A Spring Cloud e-commerce system split into an auth service plus three business
microservices (**wallet**, **shop**, **inventory**) behind an API Gateway, with a
Config Server. **No service discovery (Eureka)** — services address each other by
fixed URL.

- **Java 21** · **Maven** · **Spring Boot 4.1.1** · **Spring Cloud 2025.1.2** · **PostgreSQL (Neon)**
- Each service owns its own database (`auth_db`, `wallet_db`, `shop_db`, `inventory_db`).
- Cross-service calls use **OpenFeign** guarded by **Resilience4j** circuit breakers.

**Live gateway:** https://api-gateway-m94i.onrender.com (seeded admin: `admin` / `admin123`; free-tier services may take 30–60s to wake).

## Modules

| Module              | Port | Responsibility                                        | Database       |
|---------------------|------|-------------------------------------------------------|----------------|
| `config-server`     | 8888 | Centralised configuration (native backend)            | —              |
| `api-gateway`       | 8080 | Single entry point, routes to fixed service URLs      | —              |
| `auth-service`      | 8084 | Users, registration & login (BCrypt)                  | `auth_db`      |
| `wallet-service`    | 8081 | Wallets, deposits/withdrawals, transactions           | `wallet_db`    |
| `shop-service`      | 8082 | Products, carts, orders, payments, checkout           | `shop_db`      |
| `inventory-service` | 8083 | Products & stock, reservations                        | `inventory_db` |

### Gateway routes (no discovery)
- `/api/auth/**`      → `http://localhost:8084`
- `/api/wallet/**`    → `http://localhost:8081`
- `/api/shop/**`      → `http://localhost:8082`
- `/api/inventory/**` → `http://localhost:8083`

### Cross-service calls (Feign + Resilience4j, URL-based)
- auth → wallet   : `POST /api/wallet/wallets/{userId}` (open wallet on register)
- shop → wallet   : `POST /api/wallet/wallets/debit` · `/refund`
- shop → inventory : `POST /api/inventory/reservations` · `/confirm` · `/release`

Target URLs are configured under `services.*.url` in `auth-service` and `shop-service`.

## Security (JWT + RBAC)

Auth is stateless JWT, enforced at the **gateway**:

1. `login` / `register` return an **access token** (15 min) and a **refresh token** (7 days), signed HS256 with `app.jwt.secret` (shared by auth-service and gateway).
2. Send the access token on every call: `Authorization: Bearer <accessToken>`.
3. The gateway validates it and forwards `X-User-Id` / `X-User-Role` / `X-User-Name` to the service.
4. When the access token expires, `POST /api/auth/refresh` with the refresh token for a new pair.

Access rules at the gateway:

| Access | Endpoints |
|---|---|
| **Public** (no token) | `/api/auth/register`, `/api/auth/login`, `/api/auth/refresh`, `GET /api/shop/products/**`, `GET /api/inventory/products/**` |
| **Admin only** (`ROLE_ADMIN`) | create admins (`POST /api/auth/admin/users`); create/update/delete products; adjust stock |
| **Authenticated** (any user) | everything else — wallets, carts, orders, checkout, user lookup |

**Roles:** self-registration always creates a `ROLE_USER` (a user cannot choose their role).
The only ways to get a `ROLE_ADMIN` are the **seeded admin** (**`admin` / `admin123`**, created on
startup, configurable via `app.admin.*`) or an existing admin calling `POST /api/auth/admin/users`.
That endpoint is admin-gated at the gateway *and* re-checks `X-User-Role` in the service, so a direct
call to `:8084` cannot forge an admin.

Token lifetimes are configurable via `app.jwt.access-exp-ms` / `app.jwt.refresh-exp-ms`.
Set `APP_JWT_SECRET` in production.

**Per-user isolation:** `auth-service`, `wallet-service` and `shop-service` validate the
JWT themselves (not just the gateway), so authorization is decided by the *signed token*,
not a spoofable header. A user can only read/modify **their own** wallet, cart and orders
(the caller's id comes from the token, compared to the `{userId}` in the path); putting
someone else's id in the URL returns `403`. Admins may access any. Creating an admin
(`POST /api/auth/admin/users`) is `@PreAuthorize("hasRole('ADMIN')")`.

**Gateway-only access:** every service also requires a shared `X-Internal-Key`
(`INTERNAL_SECRET`) on all non-actuator requests. The gateway injects it automatically,
so a **direct** call to any service (bypassing the gateway) returns `403`. Inter-service
Feign calls (shop→wallet/inventory, auth→wallet) send the same key. The key must be
identical across all five services.

## Package structure (business services)

```
src/main/java/com/ejada/<service>/
├── controller/   REST endpoints
├── dto/          request/response records
├── entity/       JPA @Entity classes
├── repository/   Spring Data JPA repositories
├── service/      business logic
├── client/       Feign clients + fallbacks (auth, shop)
├── security/     JWT / internal-key filters, ownership guard
└── config/       security, Feign, exception handling
```

## API summary

### auth-service (:8084)
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/auth/register` | Create user (BCrypt) + wallet; returns JWT tokens |
| POST | `/api/auth/login` | Verify credentials; returns JWT tokens |
| POST | `/api/auth/refresh` | Exchange a refresh token for a new access token |
| GET  | `/api/auth/users/{id}` | Lookup user (authenticated) |
| POST | `/api/auth/admin/users` | **Admin only** — create a user with a role (incl. admin) |

Registration/creation validate every field (username pattern, email format,
password ≥ 8 with a letter and a digit) and return `400` with per-field messages.

`register` / `login` / `refresh` return:
`{ accessToken, refreshToken, tokenType: "Bearer", expiresInMs, user }`.

### wallet-service (:8081)
| Method | Path | Purpose |
|---|---|---|
| POST | `/api/wallet/wallets/{userId}` | Open a wallet (idempotent) |
| GET  | `/api/wallet/wallets/{userId}` | Wallet balance (auto-provisions) |
| POST | `/api/wallet/wallets/{userId}/deposit` | Add funds `{amount}` |
| POST | `/api/wallet/wallets/{userId}/withdraw` | Remove funds `{amount}` |
| GET  | `/api/wallet/wallets/{userId}/transactions` | History |
| POST | `/api/wallet/wallets/debit` · `/refund` | Called by shop for orders |

### inventory-service (:8083)
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/inventory/products[/{id}]` | Product CRUD |
| GET  | `/api/inventory/products/sku/{sku}` | Lookup by SKU |
| POST | `/api/inventory/products/{id}/stock` | Adjust stock `{delta}` |
| POST | `/api/inventory/reservations` · `/confirm` · `/release` | Reserve flow (by SKU) |

### shop-service (:8082)
| Method | Path | Purpose |
|---|---|---|
| GET/POST/PUT/DELETE | `/api/shop/products[/{id}]` | Product CRUD (create accepts `imageUrl` + optional `initialStock`, and mirrors the product into inventory) |
| GET  | `/api/shop/carts/{userId}` | View active cart |
| POST | `/api/shop/carts/{userId}/items` | Add item `{productId, quantity}` |
| DELETE | `/api/shop/carts/{userId}/items/{productId}` | Remove item |
| POST | `/api/shop/orders/checkout/{userId}` | Reserve stock → debit wallet → pay |
| GET  | `/api/shop/orders/user/{userId}` · `/{orderId}` | List / detail |

## Prerequisites

- JDK 21+ (built with JDK 26 targeting release 21)
- PostgreSQL — e.g. a [Neon](https://neon.tech) project with four databases:
  `auth_db`, `wallet_db`, `shop_db`, `inventory_db`. Each service reads its
  connection from `DB_URL` / `DB_USER` / `DB_PASSWORD` env vars (Neon URLs need
  `?sslmode=require`). Tables auto-create (`ddl-auto=update`).

## Running

All modules live under `services/`. Each needs its Neon DB env vars (same
host/user/password, different db name). Run each in its own terminal:

```bash
export DB_USER=<neon-role>
export DB_PASSWORD=<neon-password>

cd services/auth-service      && DB_URL='jdbc:postgresql://<host>/auth_db?sslmode=require'      ./mvnw spring-boot:run
cd services/wallet-service    && DB_URL='jdbc:postgresql://<host>/wallet_db?sslmode=require'    ./mvnw spring-boot:run
cd services/shop-service      && DB_URL='jdbc:postgresql://<host>/shop_db?sslmode=require'      ./mvnw spring-boot:run
cd services/inventory-service && DB_URL='jdbc:postgresql://<host>/inventory_db?sslmode=require' ./mvnw spring-boot:run
cd services/api-gateway       && ./mvnw spring-boot:run
# optional:
cd services/config-server     && ./mvnw spring-boot:run
```

All client traffic can go through the gateway at http://localhost:8080.

## Example end-to-end flow

All protected calls need `Authorization: Bearer <accessToken>` (see Security above).

```bash
# admin logs in -> capture access token
ADMIN=$(curl -s -X POST localhost:8080/api/auth/login -H 'Content-Type: application/json' \
  -d '{"username":"admin","password":"admin123"}' | jq -r .accessToken)

# admin stocks a product in inventory and lists it in the shop (same SKU, with image)
curl -X POST localhost:8080/api/inventory/products -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-1","name":"Widget","price":20,"imageUrl":"https://picsum.photos/seed/widget/400","quantityAvailable":50}'
curl -X POST localhost:8080/api/shop/products -H "Authorization: Bearer $ADMIN" -H 'Content-Type: application/json' \
  -d '{"sku":"SKU-1","name":"Widget","price":20,"category":"tools","imageUrl":"https://picsum.photos/seed/widget/400"}'

# a shopper registers (also opens a wallet) -> capture token + id
REG=$(curl -s -X POST localhost:8080/api/auth/register -H 'Content-Type: application/json' \
  -d '{"username":"loay","email":"loay@example.com","password":"secret123","fullName":"Loay"}')
USER=$(echo "$REG" | jq -r .accessToken); UID=$(echo "$REG" | jq -r .user.id)

# top up, add to cart, checkout
curl -X POST localhost:8080/api/wallet/wallets/$UID/deposit -H "Authorization: Bearer $USER" -H 'Content-Type: application/json' -d '{"amount":100}'
curl -X POST localhost:8080/api/shop/carts/$UID/items -H "Authorization: Bearer $USER" -H 'Content-Type: application/json' -d '{"productId":1,"quantity":2}'
curl -X POST localhost:8080/api/shop/orders/checkout/$UID -H "Authorization: Bearer $USER"
```

## Run it locally (for a collaborator you added to the repo)

1. Install **JDK 21+** and **Git**. Maven is bundled (`./mvnw`); **no Docker** needed.
2. Clone the repo.
3. Copy the env template and fill it in:
   ```bash
   cp .env.example .env
   ```
   - **Database:** easiest is to use the shared **Neon** databases — ask the owner for
     `DB_HOST` / `DB_USER` / `DB_PASSWORD` (the `auth_db`, `wallet_db`, `shop_db`,
     `inventory_db` databases already exist there). Or point at your own Postgres with
     those four databases (for a non‑Neon local DB, remove `?sslmode=require` in `run-all.sh`).
   - **`APP_JWT_SECRET`:** any 32+ character string (one value covers both auth and gateway).
4. Start the whole stack (use **Git Bash** on Windows):
   ```bash
   bash run-all.sh
   ```
   Tables auto‑create (`ddl-auto=update`). Wait ~40–60s (check `logs/*.log`); the gateway is
   then at http://localhost:8080. Stop everything with `bash stop-all.sh`.
5. Prefer the IDE? Open the root `pom.xml` as a Maven project in IntelliJ, put the env vars in
   each service's run config, and run the five `*Application` classes.

> The real `.env` is gitignored, so secrets never reach GitHub — a collaborator supplies their
> own (or the shared DB credentials you give them). Everything else is in the repo.

## End-to-end smoke test

Two ways to run the full admin + shopper path with assertions:

**Terminal** — `smoke-test.sh` at the repo root (needs `curl` + `python`):

```bash
BASE_URL=https://api-gateway-m94i.onrender.com bash smoke-test.sh   # live gateway
bash smoke-test.sh                                                   # defaults to http://localhost:8080
```

It warms cold free-tier services, then asserts the whole flow — admin login → create
product (auto-synced to inventory) → update price → adjust stock → create user; register
→ deposit → cart → **checkout (PAID)** → balance debited → ownership (403) and RBAC (403).
Exits non-zero on any failure.

**Postman** — the collection's **`9. E2E Smoke Flow`** folder runs the same path top-to-bottom
with `pm.test` assertions; use the folder's ▶ Run (or the Collection Runner). On the free tier,
hit **Health & Security → Gateway health** until it returns `200` first so nothing times out cold.

## API testing (Postman)

Import the collection plus the environment you want from the `postman/` folder:

- `Ejada-Ecommerce.postman_collection.json` — 50 requests across 9 folders (JWT/RBAC-aware, incl. the E2E flow)
- `Ejada-Render.postman_environment.json` — `{{baseUrl}}` = the live Render gateway
- `Ejada-Local.postman_environment.json` — `{{baseUrl}}` = `http://localhost:8080`

Run **Auth → Register** and **Login (admin)** first (**admin / admin123**) — the test
scripts capture `token`, `adminToken`, `userId`, `productId`, `sku`, and `orderId` into
collection variables, so later requests send `Authorization: Bearer …` automatically and
chain ids without editing anything. To bypass the gateway, point `{{baseUrl}}` at a service
port directly (note: services reject direct calls without the internal key).

> Configuration lives in `application.properties` per module (Spring Boot 4.1.1).

## Build all

```bash
for m in config-server api-gateway auth-service wallet-service shop-service inventory-service; do (cd "services/$m" && ./mvnw -q clean package -DskipTests); done
```
