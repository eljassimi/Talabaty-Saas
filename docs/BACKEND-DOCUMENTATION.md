# Talabaty Backend Documentation

This document explains the backend of Talabaty as if you are learning the project for the first time.  
Scope is backend only (Spring Boot API, database, integrations, jobs, and architecture).

---

## 1) Backend at a glance

Talabaty backend is a **Spring Boot (Java 17)** application organized in layered modules:

- **API Layer**: REST controllers and DTOs.
- **Domain Layer**: business logic (orders, stores, shipping, YouCan, users, etc.).
- **Core Layer**: security, scheduling, config, exceptions, storage helpers.
- **Persistence Layer**: JPA entities + repositories + Liquibase migrations.

Main entry point:

- `src/main/java/ma/talabaty/talabaty/TalabatyApplication.java`

The app enables scheduled jobs (`@EnableScheduling`) for periodic sync operations.

---

## 2) Tech stack and dependencies

From `pom.xml`, key backend technologies are:

- Spring Boot (web, security, validation, data-jpa)
- PostgreSQL
- Liquibase (database migrations)
- JWT (`jjwt`)
- Google Sheets libs + Apache POI (order sync/excel features)
- Lombok

Build/runtime assumptions:

- Java 17
- Maven wrapper (`./mvnw`)
- PostgreSQL database

---

## 3) High-level architecture

Request lifecycle (typical):

1. HTTP request reaches a controller in `api/controllers`.
2. Security filters authenticate user/API key.
3. Controller validates request and delegates to service.
4. Service applies business rules and talks to repositories/external APIs.
5. Entity changes are persisted through JPA.
6. DTO/response is returned.

### Main package map

- `api/controllers`: REST endpoints
- `api/dtos`: request/response classes
- `api/mappers`: entity <-> DTO mapping
- `domain/accounts`, `domain/users`: account/user model and services
- `domain/stores`: store management + store settings
- `domain/orders`: order lifecycle and status history
- `domain/shipping`: shipping providers + Ozon Express integration
- `domain/youcan`: YouCan OAuth and order synchronization
- `domain/credentials`: API key credentials for public API access
- `core/security`: JWT, API-key auth filters, permission helpers
- `core/scheduling`: periodic sync jobs
- `core/config`: beans/config classes

---

## 4) Configuration and environments

Main property files:

- `src/main/resources/application.properties`: default local config
- `src/main/resources/application-dev.properties`: dev profile overrides
- `src/main/resources/application-docker.properties`: docker profile overrides

Important config groups:

- **Database**: `spring.datasource.*`
- **JPA/Liquibase**: `spring.jpa.*`, `spring.liquibase.change-log`
- **JWT**: `jwt.secret`, token expiration values
- **YouCan OAuth**: `youcan.oauth.*`
- **Frontend redirect after OAuth**: `app.frontend.url`
- **WhatsApp bridge**: `whatsapp.local.url`

### Docker composition

`docker-compose.yml` defines:

- `app` (backend)
- `postgres`
- `whatsapp-bridge`

The backend in Docker uses profile `docker` and connects to postgres service internally.

---

## 5) Security architecture

Security is configured in:

- `core/security/SecurityConfig.java`

### Auth modes

Talabaty supports two authentication paths:

1. **JWT authentication**  
   Header: `Authorization: Bearer <token>`
2. **API credentials authentication**  
   Headers: `X-API-Key` + `X-API-Secret`

Filters:

- `JwtAuthenticationFilter`: validates JWT, creates `JwtUser` principal
- `ApiKeyAuthenticationFilter`: validates API key/secret and authenticates account context

### Access policy

- Stateless session management (`STATELESS`)
- Public routes include:
  - `/api/auth/**`
  - `/api/youcan/oauth/callback`
  - health/root/static routes
- Business routes are protected and role-gated by `PermissionChecker`

### Roles (conceptual)

The codebase uses roles such as:

- `PLATFORM_ADMIN`
- `ACCOUNT_OWNER`
- `MANAGER`
- `SUPPORT` (and store team support variants)

Permissions are enforced in controllers and helper classes (for example: order updates, store updates, integration management).

---

## 6) Core business modules

## 6.1 Accounts and users

Main pieces:

- `domain/accounts/model/Account.java`
- `domain/users/model/User.java`
- `api/controllers/AuthController.java`

Auth controller responsibilities:

- signup (`POST /api/auth/signup`)
- login (`POST /api/auth/login`)
- refresh token (`POST /api/auth/refresh`)
- change password (`POST /api/auth/change-password`)

Signup flow (simplified):

1. Create account.
2. Create first user as `ACCOUNT_OWNER`.
3. Return access and refresh tokens.

---

## 6.2 Stores

Main pieces:

- `domain/stores/model/Store.java`
- `domain/stores/model/StoreSettings.java`
- `api/controllers/StoreController.java`
- `domain/stores/service/StoreService.java`

Store module handles:

- CRUD operations for stores
- Role-based visibility and update permission
- Store settings (WhatsApp templates, support revenue settings, etc.)
- Store-level shipping provider configuration

Important design point:

- Stores are scoped by account, and many operations verify store-account ownership before acting.

---

## 6.3 Orders

Main pieces:

- `domain/orders/model/Order.java`
- `domain/orders/model/OrderStatusHistory.java`
- `api/controllers/OrderController.java`
- `domain/orders/service/OrderService.java`

Order service responsibilities:

- Create order
- Prevent duplicates by `(storeId, externalOrderId)` when external ID is provided
- Update order fields
- Update order status + write status history
- Optional side effects:
  - WhatsApp automation on status changes
  - Support revenue credit entries on confirmation

### Assignment behavior for support teams

When orders are listed for a store, unassigned orders can be distributed to support members in a round-robin manner (`distributeUnassignedOrdersToSupport`).

---

## 6.4 Public API and API credentials

Main pieces:

- `api/controllers/PublicApiController.java`
- `api/controllers/ApiCredentialController.java`
- `domain/credentials/service/ApiCredentialService.java`

Purpose:

- Allow external systems to create/check orders using API key headers.
- Credentials are generated per account and can be revoked.

Important:

- Public API endpoints are authenticated (not anonymous), but can be authenticated by API key instead of JWT.

---

## 7) Delivery provider integration (Ozon Express)

This project currently integrates with **Ozon Express**.

Main files:

- `domain/shipping/model/ShippingProvider.java`
- `domain/shipping/service/ShippingProviderService.java`
- `domain/shipping/service/OzonExpressService.java`
- `api/controllers/ShippingController.java`
- `api/controllers/StoreController.java` (store-level provider setup)

### 7.1 Data model

`ShippingProvider` stores:

- account
- optional store reference
- provider type (`OZON_EXPRESS`)
- `customerId`
- `apiKey`
- active flag, display name, timestamps

### 7.2 Account-level vs store-level providers

Two ways to configure provider credentials:

- **Account-level** through `/api/shipping/providers`
- **Store-level** through `/api/stores/{id}/shipping-providers`

For sending orders from the order workflow, the code uses **store-level provider lookup** (`getActiveProviderForStore`), which means each store should have its own active Ozon credentials.

### 7.3 Delivery provider setup (recommended flow)

1. Create a store.
2. Configure shipping provider for that store:
   - `POST /api/stores/{storeId}/shipping-providers`
   - Body includes `customerId`, `apiKey`, optional `displayName`
3. Validate cities:
   - `GET /api/shipping/ozon-express/cities`
4. Send orders to shipping:
   - Single: `POST /api/orders/{id}/send-to-shipping`
   - Batch: `POST /api/orders/batch/send-to-shipping`

### 7.4 Sending an order to Ozon

When sending order to shipping:

1. Backend validates order ownership (same account).
2. Fetches active store-level Ozon credentials.
3. Maps order fields to Ozon parcel payload:
   - receiver, phone, city ID, address, price, options
4. Calls Ozon endpoint via `OzonExpressService.createParcel(...)`.
5. Extracts tracking number from response (supports multiple response shapes).
6. Saves tracking number on order (`ozonTrackingNumber`).

### 7.5 Delivery note (Bon de Livraison) flow

`ShippingController` supports complete BL generation:

- Create BL
- Add parcel tracking numbers
- Save BL
- Return PDF URLs (or proxy endpoint for download)

Endpoint:

- `POST /api/shipping/ozon-express/delivery-notes/create-full`

It can reuse an existing `deliveryNoteRef` when all selected orders already share one.

---

## 8) YouCan integration

YouCan integration lets Talabaty connect a store to YouCan via OAuth and sync orders automatically.

Main files:

- `api/controllers/YouCanController.java`
- `domain/youcan/service/YouCanOAuthService.java`
- `domain/youcan/service/YouCanApiService.java`
- `domain/youcan/service/YouCanOrderSyncService.java`
- `core/scheduling/YouCanOrderSyncScheduler.java`

## 8.1 Connection model

`YouCanStore` entity stores:

- Talabaty account + store relation
- YouCan store identifiers and domain/name
- access token + refresh token
- token expiration
- active flag
- last sync time

## 8.2 OAuth connection flow

1. Client calls:
   - `GET /api/youcan/connect/{storeId}`
2. Backend builds YouCan authorization URL with:
   - client ID
   - redirect URI
   - state (`accountId:storeId`)
3. User authorizes on YouCan.
4. YouCan redirects to:
   - `GET /api/youcan/oauth/callback?code=...&state=...`
5. Backend exchanges code for token.
6. Backend reads YouCan store info.
7. Backend creates/updates `YouCanStore` record and marks active.
8. Backend redirects browser to frontend store page using `app.frontend.url`.

## 8.3 Token management

`YouCanOAuthService.getValidAccessToken(...)` refreshes token automatically when expired (if refresh token exists).

## 8.4 Manual sync flow

Manual endpoint:

- `POST /api/youcan/stores/{youcanStoreId}/sync`

Behavior:

1. Verify store belongs to authenticated account.
2. Pull YouCan orders (`listOrders`) with filters based on `lastSyncAt`.
3. For each order:
   - map customer/address/city/price/product fields
   - upsert in Talabaty using external order ID
4. Update `lastSyncAt`.

## 8.5 Automatic sync scheduler

`YouCanOrderSyncScheduler` runs every 5 minutes:

- Loads active YouCan store connections
- Calls sync service for each
- Logs successes/failures

---

## 9) Scheduled jobs (backend automation)

From core scheduling package:

- `YouCanOrderSyncScheduler`: every 5 minutes
- `GoogleSheetsSyncScheduler`: every 30 seconds (for spreadsheet sync module)

This means backend has ongoing background processes; it is not only request/response APIs.

---

## 10) Data and persistence

Persistence uses:

- JPA entities (`@Entity`)
- repositories (`JpaRepository` style)
- Liquibase changelog:
  - `src/main/resources/db/changelog/db.changelog-master.yaml`

Important behavior patterns:

- Entity relationships are used heavily (order -> store -> account).
- Most account isolation checks are done in controller/service level before modifying data.
- Some updates use `saveAndFlush` when immediate persistence is needed.

---

## 11) Important backend flows (student-friendly walkthrough)

### A) Standard order lifecycle

1. Authenticated user creates order in a store.
2. Order starts in initial status (`ENCOURS`).
3. Status changes are done through update endpoint.
4. Every status change creates history row.
5. Optional automations:
   - WhatsApp notification
   - support revenue crediting
6. Order can be sent to shipping provider and gets tracking number.

### B) External platform order import (YouCan)

1. Connect Talabaty store to YouCan (OAuth).
2. Sync job fetches YouCan orders.
3. Mapping logic converts YouCan payload to Talabaty order schema.
4. Existing orders are updated; new ones are created.
5. Metadata keeps YouCan references (`youcan_order_id`, store domain, etc.).

### C) Delivery execution with Ozon

1. Store owner configures Ozon credentials.
2. Support/manager sends order(s) to shipping.
3. Ozon returns tracking numbers.
4. Tracking stored on orders for future operations.
5. BL can be generated from selected tracked orders.

---

## 12) Local setup and run (backend)

## Option 1: Docker (quickest full stack for backend dependencies)

From project root:

```bash
docker compose up --build
```

Services:

- backend on `http://localhost:8080`
- postgres (mapped host `5433 -> container 5432`)
- whatsapp bridge on `3100`

## Option 2: Run backend locally

Requirements:

- Java 17
- PostgreSQL running with matching credentials from `application.properties` (or override env vars)

Run:

```bash
./mvnw spring-boot:run
```

For dev profile (useful with separate frontend and YouCan redirect):

```bash
SPRING_PROFILES_ACTIVE=dev ./mvnw spring-boot:run
```

Then API is available on `http://localhost:8080`.

---

## 13) Integration setup cookbook

## 13.1 Setup YouCan integration

1. Ensure YouCan OAuth values are configured:
   - `youcan.oauth.client-id`
   - `youcan.oauth.client-secret`
   - `youcan.oauth.redirect-uri`  
     (must exactly match app config in YouCan dashboard)
2. Create/login account and create a store.
3. Call `GET /api/youcan/connect/{storeId}`.
4. Open returned `authorizationUrl` in browser.
5. Approve access in YouCan.
6. Verify callback and redirection worked.
7. Confirm connection via `GET /api/youcan/stores`.
8. Trigger manual sync endpoint to test import.

## 13.2 Setup delivery provider (Ozon Express)

1. Get Ozon `customerId` and `apiKey`.
2. Configure store provider:
   - `POST /api/stores/{storeId}/shipping-providers`
3. Check configured providers:
   - `GET /api/stores/{storeId}/shipping-providers`
4. Retrieve city list:
   - `GET /api/shipping/ozon-express/cities`
5. Send one order to shipping:
   - `POST /api/orders/{id}/send-to-shipping` with `cityId`
6. Verify order has tracking number.
7. For operations/logistics, generate delivery note from tracked orders.

---

## 14) Known design observations (important for maintainers)

- Some services include heavy debug logging (`System.out.println` / `System.err.println`).
- Account/store ownership checks are enforced mostly in application logic (controllers/services), not by DB row-level security.
- There are both account-level and store-level provider APIs; shipping from order flow expects store-level provider.
- OAuth callback route is intentionally public and protected by state/code validation logic.

---

## 15) Useful backend files to study first

If you are onboarding, start in this order:

1. `TalabatyApplication.java`
2. `core/security/SecurityConfig.java`
3. `api/controllers/AuthController.java`
4. `api/controllers/StoreController.java`
5. `api/controllers/OrderController.java`
6. `domain/orders/service/OrderService.java`
7. `api/controllers/ShippingController.java`
8. `domain/shipping/service/OzonExpressService.java`
9. `api/controllers/YouCanController.java`
10. `domain/youcan/service/YouCanOAuthService.java`
11. `domain/youcan/service/YouCanOrderSyncService.java`

---

## 16) Short glossary

- **Account**: top-level tenant/customer in Talabaty.
- **Store**: business unit under an account.
- **Order**: customer purchase/delivery order managed by support teams.
- **Status history**: timeline of order status changes.
- **Shipping provider**: credentials/config used to call delivery API (Ozon).
- **YouCan store connection**: OAuth-linked e-commerce source store for imports.
- **BL (Bon de Livraison)**: delivery note generated in Ozon process.

