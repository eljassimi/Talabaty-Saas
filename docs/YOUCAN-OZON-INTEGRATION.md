# YouCan and Ozon Integration Guide

This document explains how the two main external integrations work in Talabaty:

- **YouCan** (OAuth connection + order sync into Talabaty)
- **Ozon Express** (shipping provider setup + parcel and delivery note workflow)

It is written to help you understand the codebase quickly and safely modify it.

---

## 1) High-level architecture

Talabaty integration logic is split into three layers:

1. **Controller layer** (`api/controllers`)  
   Exposes REST endpoints used by frontend/admin users.
2. **Service layer** (`domain/.../service`)  
   Contains integration business rules and HTTP calls to external APIs.
3. **Persistence layer** (`domain/.../model` + repositories)  
   Stores connected stores, credentials, sync markers, and shipping providers.

Security is enforced using authenticated `Authentication` objects and account ownership checks before integration actions run.

---

## 2) YouCan integration

### 2.1 Main classes

- `YouCanController`: public API endpoints for connect/callback/sync/disconnect.
- `YouCanOAuthService`: OAuth URL generation, code exchange, token refresh, store info retrieval.
- `YouCanApiService`: wrapper around YouCan API calls (`/orders`, `/customers`, order actions).
- `YouCanOrderSyncService`: maps YouCan payloads to Talabaty `Order`.
- `YouCanOrderSyncScheduler`: background periodic sync for active connected stores.
- `YouCanStore` model/repository: stores integration state for one Talabaty store.

### 2.2 OAuth connection flow

Flow from frontend button click to saved connection:

1. Frontend calls `GET /api/youcan/connect/{storeId}`.
2. `YouCanController` checks integration permissions (`canAccessIntegrations`) and extracts account/store ids.
3. `YouCanOAuthService.getAuthorizationUrl()` builds YouCan authorize URL with:
   - `client_id`
   - `redirect_uri`
   - `response_type=code`
   - `state=<accountId>:<storeId>`
   - optional `scope[]`
4. User authorizes in YouCan UI.
5. YouCan redirects to `/api/youcan/oauth/callback?code=...&state=...`.
6. `handleOAuthCallback()` exchanges code for tokens and stores connection in `YouCanStore`.
7. Backend redirects user to frontend store page with `?youcan=connected` or `?youcan=error`.

### 2.3 Token and store metadata handling

In `YouCanOAuthService.handleOAuthCallback()`:

- Exchanges `code` via `POST https://api.youcan.shop/oauth/token`.
- Extracts `access_token`, `refresh_token`, `expires_in`.
- Calls `GET https://api.youcan.shop/stores/me` to fetch store identity.
- Saves/updates `YouCanStore` fields:
  - account + Talabaty store link
  - YouCan store id/name/domain
  - access/refresh token
  - token expiration
  - scopes
  - active flag

When API calls are made later, `getValidAccessToken()` auto-refreshes expired tokens (if refresh token exists).

### 2.4 Order sync flow

Manual sync endpoint:

- `POST /api/youcan/stores/{youcanStoreId}/sync`

Scheduled sync:

- `YouCanOrderSyncScheduler` runs every 5 minutes (`fixedRate=300000`) for all active connected stores.

Core behavior in `YouCanOrderSyncService.syncOrdersFromYouCanStore()`:

1. Load target `YouCanStore`.
2. Build incremental filters:
   - if previous sync exists -> `updated_at_min=lastSyncAt`
   - otherwise -> `created_at_min=now - 30 days`
3. Call `YouCanApiService.listOrders(...)`.
4. For each order:
   - resolve external order id
   - fetch customer details if available
   - extract normalized fields (name, phone, city, address, total, currency, product info)
   - map status to internal `OrderStatus`
   - upsert into Talabaty `Order` using `externalOrderId` + `storeId`
5. Save `lastSyncAt`.

### 2.5 Data mapping notes

Mapping is defensive because YouCan payloads can vary:

- Customer and address may appear in `customer`, `shipping.address`, or `payment.address`.
- City extraction tries multiple keys (`city`, `city_name`, `region`, nested `location`, etc.).
- If city still missing, service fetches full order + customer again (`enrichCityFromFullOrder`).
- Product names are read from variants and concatenated.

This makes sync resilient to payload shape changes.

### 2.6 Security and tenancy checks

`YouCanController` ensures:

- user is authenticated
- user role can access integrations
- target `YouCanStore` belongs to authenticated account

This prevents cross-account integration misuse.

### 2.7 Useful YouCan endpoints summary

- `GET /api/youcan/connect/{storeId}` -> returns OAuth authorization URL
- `GET /api/youcan/oauth/callback` -> receives OAuth redirect
- `GET /api/youcan/stores` -> list connected YouCan stores for account
- `POST /api/youcan/stores/{youcanStoreId}/sync` -> force sync
- `DELETE /api/youcan/stores/{youcanStoreId}` -> soft disconnect (`active=false`)

---

## 3) Ozon Express shipping provider integration

### 3.1 Main classes

- `ShippingController`: shipping API endpoints.
- `ShippingProviderService`: CRUD and lookup of provider credentials.
- `OzonExpressService`: HTTP client to Ozon Express API.
- `ShippingProvider` entity: stores customer id, api key, scope (account/store), active flag.
- `ProviderType`: currently supports `OZON_EXPRESS`.

### 3.2 Provider configuration model

A shipping provider can be linked to:

- an **account** (global for all stores), and optionally
- a **specific store**.

Saved fields:

- `providerType` (currently Ozon only)
- `customerId`
- `apiKey`
- `displayName`
- `active`

Credentials are then reused by shipping endpoints.

### 3.3 Parcel creation flow

Endpoint:

- `POST /api/shipping/ozon-express/parcels`

Flow:

1. Resolve authenticated account.
2. Load active Ozon provider (`getActiveProvider(accountId, OZON_EXPRESS)`).
3. Map request DTO to `OzonExpressService.CreateParcelRequest`.
4. Call Ozon endpoint:  
   `POST https://api.ozonexpress.ma/customers/{customerId}/{apiKey}/add-parcel`
5. Validate Ozon response and detect nested error payloads.
6. If request contains `orderId` and Ozon returns tracking number, save it into order (`order.ozonTrackingNumber`).

Important request fields:

- receiver, phone, city id, address
- parcel price (rounded to integer in service)
- stock/open/fragile/replace flags
- optional products payload

### 3.4 Tracking and parcel info

Endpoints:

- `POST /api/shipping/ozon-express/parcels/info`
- `POST /api/shipping/ozon-express/parcels/track`

`track` supports:

- one tracking number
- multiple tracking numbers in one request

The controller validates at least one tracking number exists before calling Ozon.

### 3.5 Delivery note (Bon de Livraison) flow

Simple endpoints:

- create note -> `/ozon-express/delivery-notes`
- add parcels -> `/ozon-express/delivery-notes/{ref}/parcels`
- save note -> `/ozon-express/delivery-notes/{ref}/save`

Combined endpoint:

- `POST /api/shipping/ozon-express/delivery-notes/create-full`

`create-full` orchestrates the full process:

1. Validate `orderIds` input.
2. Load all orders and enforce same-account ownership.
3. Collect distinct Ozon tracking numbers from orders.
4. If all selected orders already share one `deliveryNoteRef`, return existing PDF links.
5. Else:
   - create delivery note in Ozon
   - extract `ref`
   - add tracking numbers to note
   - save note
6. Persist resulting `deliveryNoteRef` back on all selected orders.
7. Return generated links:
   - standard BL PDF
   - A4 tickets PDF
   - 4x4 tickets PDF

### 3.6 PDF retrieval strategy

Endpoint:

- `GET /api/shipping/ozon-express/delivery-notes/pdf?ref=...&type=...`

Service behavior:

1. Try download from Ozon client URL without credentials.
2. If response is HTML or empty, retry with `customer_id` and `api_key` query parameters.
3. If still unavailable, return explicit error asking to download from Ozon portal.

### 3.7 City resolution support

Endpoint:

- `GET /api/shipping/ozon-express/cities`

Used by frontend to fetch allowed city ids before creating parcel.  
Helpful when Ozon returns `City Not Found`.

---

## 4) Integration boundaries and common pitfalls

### 4.1 YouCan pitfalls

- OAuth callback state must parse as `<accountId>:<storeId>`.
- Missing/expired tokens trigger refresh path.
- Payload shapes can change; extraction logic intentionally tries multiple locations.
- Sync can skip immediate updates if local order was just modified (throttling guard in update path).

### 4.2 Ozon pitfalls

- Invalid `cityId` is a frequent cause of parcel creation failure.
- Ozon can return nested error structures; service parses deeply to surface clear messages.
- Missing tracking numbers blocks delivery note creation.
- Some BL/PDF flows depend on Ozon-side state and may need portal fallback.

---

## 5) End-to-end examples (mental model)

### Example A: connect YouCan and import orders

1. User opens store page and clicks connect.
2. Backend returns OAuth URL.
3. User authorizes.
4. Callback stores tokens and linked YouCan store.
5. User triggers manual sync (or waits scheduler).
6. Orders are upserted in Talabaty with `OrderSource.YOUCAN`.

### Example B: send order to Ozon and generate BL

1. Account configures Ozon `customerId + apiKey`.
2. User sends order data to `/parcels` -> Ozon returns tracking number.
3. Tracking number is saved into Talabaty order.
4. User selects orders and calls `/delivery-notes/create-full`.
5. Backend creates BL, binds parcels, saves BL, stores `deliveryNoteRef`, returns PDF links.

---

## 6) Suggested code reading order

To understand quickly, read in this order:

1. `YouCanController`
2. `YouCanOAuthService`
3. `YouCanApiService`
4. `YouCanOrderSyncService`
5. `ShippingController`
6. `ShippingProviderService`
7. `OzonExpressService`
8. `ShippingProvider` + repositories

---

## 7) Quick troubleshooting checklist

### YouCan

- Check OAuth credentials and redirect URI config.
- Verify callback query has both `code` and `state`.
- Confirm `YouCanStore.active=true` and token not expired (or refresh token exists).
- Test `POST /api/youcan/stores/{id}/sync` manually and inspect logs.

### Ozon

- Verify account has active provider credentials.
- Validate city id from `/api/shipping/ozon-express/cities`.
- Ensure order has tracking number before BL creation.
- If PDF fails through API, use Ozon portal link directly.

---

If you want, I can generate a second file with concrete request/response JSON examples for each endpoint to use as a team onboarding cheat sheet.
