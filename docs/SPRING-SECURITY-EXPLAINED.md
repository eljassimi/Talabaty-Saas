# Spring Security in Talabaty — A Student Guide

This document explains **how security works in this project** using **Spring Security**. It is written for someone who is **starting to understand** Spring Security: what problems it solves, how a request flows through the server, and how **login**, **tokens**, **roles**, and **permissions** fit together.

---

## 1. What problem does Spring Security solve?

When you build a web API (REST), you need to answer two questions for almost every request:

| Question | Name | Example |
|----------|--------|---------|
| **Who is this user?** | **Authentication** | “This is user `abc-123` logged in as `owner@shop.com`.” |
| **What may they do?** | **Authorization** | “They may view their stores but not delete another account’s data.” |

**Spring Security** is a framework that sits **in front of your controllers**. It runs a **chain of filters** on each HTTP request. Those filters can:

- Block unauthenticated access to protected URLs  
- Let public URLs through without login  
- Run **your** custom code (for example: read a JWT from a header and fill in “who is logged in”)  

So you don’t put `if (user == null)` in every controller by hand; security is **centralized** and **consistent**.

---

## 2. Core ideas you must remember

### 2.1 Stateless API + JWT (no server-side session for “who is logged in”)

In a **classic** web app, the server might store a **session** after login (cookie + server memory).  

In **this** project, the API is configured as **stateless**: the server does **not** keep a session that says “user X is logged in.” Instead, the client sends a **JWT (JSON Web Token)** on each request. The token is **signed** so the server can verify it was issued by your app and was not tampered with.

That matches this line in `SecurityConfig`:

- `SessionCreationPolicy.STATELESS` — Spring Security does not create HTTP sessions for authentication in the usual way.

### 2.2 Filter chain (order matters)

Spring Security does **not** jump straight to your `@RestController`. The request passes through **filters** first. Think of a **pipeline**:

```text
HTTP request
    → Security filters (CSRF, session, authorization rules, …)
    → Your custom filters (JWT, API key, …)
    → DispatcherServlet
    → Your controller method
```

In this project, **two custom filters** are registered **before** the standard username/password filter:

1. **`ApiKeyAuthenticationFilter`** — for integrations using `X-API-Key` / `X-API-Secret` (when there is no Bearer JWT).  
2. **`JwtAuthenticationFilter`** — reads `Authorization: Bearer <token>`, validates the JWT, and sets **who is authenticated** for this request.

See `SecurityConfig` for `addFilterBefore(...)`.

### 2.3 `SecurityContextHolder` — “current user” for this request

After the JWT filter succeeds, it puts an **authentication object** into **`SecurityContextHolder`**.  

For the rest of that single request, your code can ask: “Who is logged in?” — for example by injecting `Authentication` into a controller method.

This is how controllers know the **user id** and **account id** without you passing them as insecure query parameters from the client alone.

---

## 3. What is configured in `SecurityConfig`?

Open `SecurityConfig.java`. In simple terms it does the following:

| Setting | Meaning in this project |
|--------|-------------------------|
| **`PasswordEncoder` → BCrypt** | Passwords are **hashed** before storage; login compares using BCrypt, not plain text. |
| **CSRF disabled** | Common for **token-based** REST APIs where the client sends `Authorization` headers, not browser form posts. (Your teacher may ask: CSRF mainly targets **cookie + session** browser flows.) |
| **`authorizeHttpRequests`** | Defines which URL patterns are **public** vs need **authentication**. |
| **Custom filters** | JWT and API key authentication run as part of the chain. |

**Typical rules in this project:**

- **Public (no login):** e.g. `/`, `/api/health`, `/api/auth/**` (signup, login, refresh), and some OAuth callbacks.  
- **Authenticated:** most other `/api/**` routes.  
- **Non-API static/SPA paths:** paths that do **not** start with `/api` are often allowed so the frontend can load; API data still requires tokens when calling `/api/...`.

So: **Spring Security first checks “is this URL allowed without login?”** If the route requires authentication and there is no valid JWT/API key, the request is rejected **before** your business logic runs.

---

## 4. Authentication: from login to JWT

### 4.1 Signup and login (`AuthController`)

- **`POST /api/auth/signup`** — creates account + user, returns **access** and **refresh** tokens.  
- **`POST /api/auth/login`** — checks email/password, then returns tokens.  

Tokens are created in **`JwtTokenProvider`**:

- **Access token** includes claims such as `userId`, `email`, `accountId`.  
- Tokens are **signed** with a secret (`jwt.secret` in configuration). Anyone who knows the secret can create valid tokens — so in **production** the secret must be strong and kept private.

### 4.2 Using the access token

The client sends:

```http
Authorization: Bearer <access_token>
```

**`JwtAuthenticationFilter`**:

1. Reads the `Authorization` header.  
2. If it starts with `Bearer `, it takes the token string.  
3. Calls **`JwtTokenProvider.validateToken(...)`** — checks signature, expiry, etc.  
4. If invalid → responds with **401 Unauthorized** and a JSON error (does not continue to the controller).  
5. If valid → reads `userId`, `email`, `accountId` from the token and builds a **`JwtUser`** principal.  
6. Sets **`UsernamePasswordAuthenticationToken`** in **`SecurityContextHolder`**.

After that, the controller can use **`Authentication`** to know **which user** and **which account** the request belongs to.

### 4.3 Refresh token

**`POST /api/auth/refresh`** accepts a refresh token, validates it, loads the user from the database, and issues **new** access and refresh tokens.  

Conceptually: **access token** = short-lived proof of login; **refresh token** = longer-lived, used only to get new access tokens (still protect both in transit and in storage).

---

## 5. Authorization: roles and `PermissionChecker`

**Spring Security’s URL rules** in this project mostly say: “you must be **authenticated**.”  

**Fine-grained rules** (who may create a store, delete a store, update an order, etc.) are implemented in **`PermissionChecker`** and in controllers that call it.

### 5.1 Roles (`UserRole`)

The enum includes roles such as:

- `PLATFORM_ADMIN`  
- `ACCOUNT_OWNER`  
- `MANAGER`  
- `SUPPORT`  

These represent **business roles**, not Spring’s built-in `ROLE_*` strings unless you map them (this project often checks **enum** in Java code).

### 5.2 What `PermissionChecker` does

`PermissionChecker` contains methods like:

- `canCreateStore(role)`  
- `canViewStore(role, userId, store)`  
- `canUpdateOrder(role, userId, order)`  
- …  

Controllers load the **`User`** from the database using the authenticated **user id**, read **`user.getRole()`**, then call **`permissionChecker.can...(...)`**.  

If the check fails, the controller throws **`AccessDeniedException`** → user gets **403 Forbidden** (handled by your exception handler).

So in an exam you can say:

> “URL-level security is handled by Spring Security (authenticated or not).  
> **Application-level authorization** is enforced in services/controllers with **`PermissionChecker`** based on **role** and sometimes **resource ownership** (e.g. manager of this store, support assigned to this order).”

### 5.3 Example pattern (stores)

In `StoreController`, creating a store:

1. Get **user id** from authentication (`AuthenticationHelper.getUserIdFromAuth`).  
2. Load **User** → get **role**.  
3. If `!permissionChecker.canCreateStore(role)` → **access denied**.  
4. Otherwise get **account id** from auth and create the store **for that account**.

This combines **authentication** (who) with **authorization** (what they may do).

---

## 6. Multi-tenant safety (`accountId`)

The JWT carries **`accountId`**. **`AuthenticationHelper`** extracts it and turns it into a **UUID**.

Many operations first filter by **account** (e.g. “stores belonging to this account”). That reduces the risk that user A’s token is used to manipulate user B’s data — **as long as** every query uses the account id from the **verified token**, not a client-supplied account id alone.

---

## 7. API key authentication (integrations)

**`ApiKeyAuthenticationFilter`** runs when there is **no** `Bearer` token:

- Reads **`X-API-Key`** and **`X-API-Secret`**.  
- Validates via **`ApiCredentialService`**.  
- On success, sets authentication with principal = **account id** (string), for machine-to-machine or integration scenarios.

So the same backend supports:

- **Humans** → JWT (`Authorization: Bearer ...`)  
- **Integrations** → API key + secret headers  

Your teacher may ask: **API keys must be kept secret** (like passwords); use HTTPS in production.

---

## 8. Glossary (quick revision)

| Term | Short meaning |
|------|----------------|
| **Authentication** | Proving identity (login, token). |
| **Authorization** | Deciding allowed actions (roles, permissions). |
| **Filter chain** | Ordered steps Spring runs on each request. |
| **JWT** | Signed token carrying claims (userId, accountId, …). |
| **Stateless** | Server does not rely on server-side session for “logged in”; token proves it each time. |
| **BCrypt** | Password hashing algorithm (one-way, with salt). |
| **Principal** | The “current user” object in `Authentication` (here often `JwtUser`). |
| **403 vs 401** | 401 = not authenticated / bad token; 403 = authenticated but not allowed. |

---

## 9. How to explain it in one minute (oral exam)

You can say:

1. **Spring Security** protects HTTP endpoints with a **filter chain** and rules for which URLs need login.  
2. We use **JWT**: login returns tokens; the client sends **Bearer** token; **`JwtAuthenticationFilter`** validates and fills **`SecurityContextHolder`**.  
3. **Passwords** are hashed with **BCrypt**.  
4. **Roles and business rules** are checked with **`PermissionChecker`** in controllers/services, not only by “any logged-in user.”  
5. **`accountId` in the token** helps **isolate tenants** so users work within their account.  
6. **API keys** are an alternative auth path for **integrations**.

---

## 10. Further reading (official)

- [Spring Security reference](https://docs.spring.io/spring-security/reference/index.html) — Servlet architecture and filter chain.  
- [Spring Security authorization](https://docs.spring.io/spring-security/reference/servlet/authorization/index.html) — `authorizeHttpRequests`, expressions, method security.

---

*This guide describes the Talabaty codebase patterns. Always align explanations with the actual code your teacher can open (`SecurityConfig`, `JwtAuthenticationFilter`, `JwtTokenProvider`, `PermissionChecker`, `AuthController`).*
