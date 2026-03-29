# Docker and GitHub Actions (CI) — Talabaty

This document describes how **containerization** and **continuous integration** are set up in this repository.

---

## 1. Docker overview

The app is a **single Spring Boot JAR** that serves:

- The **REST API** under `/api`
- The **built SPA** (Vite/React) from `classpath:/static` (produced during the Docker build)

PostgreSQL and a **WhatsApp bridge** run as **separate containers** in `docker-compose.yml`.

---

## 2. Root `Dockerfile` (multi-stage)

The root `Dockerfile` has **three stages**.

### Stage 1 — `frontend-builder` (Node 20)

- **Purpose:** Build the Vite frontend.
- **Steps:**
  - Copies `talabaty-frontend/package.json` and `package-lock.json`, runs `npm ci`.
  - Copies the rest of `talabaty-frontend/`.
  - Sets `VITE_API_URL=/api` so the browser calls the **same origin** as the UI; the backend proxies or serves API routes as configured.
  - Runs `npx vite build` → output in `dist/`.

### Stage 2 — `builder` (Eclipse Temurin JDK 17)

- **Purpose:** Compile the Spring Boot app and embed the frontend.
- **Steps:**
  - Copies Maven wrapper, `pom.xml`, runs `./mvnw dependency:go-offline` for better layer caching.
  - Copies `src/`.
  - **Copies** `dist/` from stage 1 into **`src/main/resources/static/`** so static files are packaged inside the JAR.
  - Runs `./mvnw package -DskipTests -B` → produces `target/*.jar`.

### Stage 3 — runtime (Eclipse Temurin JRE 17)

- **Purpose:** Minimal image to run the JAR.
- Creates a non-root user (`app`, UID/GID 1001).
- Copies only the built JAR as `app.jar`.
- **Exposes** port `8080`.
- **Entrypoint:** `java -jar app.jar`.

**Note:** Tests are skipped in the image build (`-DskipTests`). CI runs tests separately in `./mvnw verify`.

---

## 3. `docker-compose.yml`

| Service           | Role |
|-------------------|------|
| **`postgres`**    | PostgreSQL 16. DB `talabaty`, user/password `postgres`/`postgres`. Host port **5433** → container **5432**. Healthcheck: `pg_isready`. |
| **`whatsapp-bridge`** | Node service (Puppeteer/Chromium) for WhatsApp; built from `./whatsapp-bridge`. Port **3100**. Volume persists `.wwebjs_auth`. |
| **`app`**         | Spring Boot image built from the root `Dockerfile`. Port **8080**. Depends on healthy `postgres` and started `whatsapp-bridge`. |

### `app` environment (compose)

- `SPRING_PROFILES_ACTIVE=docker` — loads `application-docker.properties` (currently sets `server.address=0.0.0.0` so the process listens on all interfaces inside the container).
- `SPRING_DATASOURCE_*` — points JDBC at `jdbc:postgresql://postgres:5432/talabaty`.
- `WHATSAPP_LOCAL_URL=http://whatsapp-bridge:3100` — in-container URL to the bridge.
- `APP_FRONTEND_URL` — defaults to `http://localhost:8080`; override with env file or shell if needed (e.g. public URL for OAuth redirects).

### Volumes

- `postgres-data` — database files.
- `app-uploads` — mounted at `/app/uploads` for the app’s upload directory (align with `file.upload-dir` if you override it for Docker).
- `whatsapp-bridge-auth` — WhatsApp session data for the bridge.

### Typical local run

From the repository root:

```bash
docker compose up --build
```

Then open **http://localhost:8080** for the UI/API.

---

## 4. WhatsApp bridge `Dockerfile`

Located under `whatsapp-bridge/`:

- Base: `node:18-bookworm-slim`.
- Installs **Chromium** and fonts for Puppeteer; uses system Chromium (`PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true`).
- `npm install --omit=dev`, copies `server.js`, exposes **3100**.

Compose builds this image as service `whatsapp-bridge`.

---

## 5. GitHub Actions — `.github/workflows/ci.yml`

**Workflow name:** `CI`

### When it runs (`on`)

- **Push** to `main` or `master`
- **Pull requests** targeting `main` or `master`

### Environment variables

- `JAVA_VERSION`: `17`
- `NODE_VERSION`: `20`

### Jobs

1. **`backend`**
   - Checkout, `chmod +x mvnw`
   - **setup-java** (Temurin 17, Maven cache)
   - **`./mvnw verify -B`** — compiles, runs tests, runs checks (full Maven verify lifecycle)

2. **`frontend`**
   - Checkout
   - **setup-node** (Node 20, npm cache from `talabaty-frontend/package-lock.json`)
   - `npm ci` in `talabaty-frontend`
   - `npm run build` (TypeScript + Vite)

3. **`docker`**
   - Runs only after **`backend`** and **`frontend`** succeed (`needs: [backend, frontend]`).
   - **Docker Buildx** with **GitHub Actions cache** (`cache-from` / `cache-to: type=gha`).
   - Builds **two images locally** (`push: false`, `load: true`):
     - Root context → tag `talabaty-backend:latest` (same Dockerfile as local compose `app` build).
     - Context `./whatsapp-bridge` → tag `talabaty-whatsapp-bridge:latest`.

### What CI does **not** do today (CD)

- Images are **not pushed** to a registry.
- There is **no deploy** job.

The workflow file includes a **commented example** `push-image` job showing how you could push to **GHCR** (`ghcr.io`) on pushes to `main`/`master` using `GITHUB_TOKEN`. Enable and adjust tags/names when you are ready for CD.

---

## 6. How Docker build relates to CI

| Concern | Local / Compose | CI |
|--------|------------------|-----|
| Backend tests | Run manually: `./mvnw verify` | `backend` job |
| Frontend build | `npm run build` in `talabaty-frontend` | `frontend` job |
| Full stack image | `docker compose build app` or root `docker build` | `docker` job builds same contexts |

CI **does not** run `docker compose up` or integration tests against containers; it only **verifies** that images **build successfully**.

---

## 7. Operational checklist

- **Secrets:** Do not rely on default JWT/DB passwords in production; use env vars or secrets management.
- **YouCan OAuth:** `app.frontend.url` / `APP_FRONTEND_URL` and YouCan redirect URIs must match your public URL when not using localhost.
- **Database migrations:** Liquibase runs with the app; ensure `SPRING_DATASOURCE_*` in compose matches your Postgres service.

---

## 8. File reference

| Path | Purpose |
|------|---------|
| `Dockerfile` | Multi-stage: frontend → Spring Boot JAR → JRE runtime |
| `docker-compose.yml` | `app`, `postgres`, `whatsapp-bridge` + volumes |
| `whatsapp-bridge/Dockerfile` | WhatsApp bridge container |
| `src/main/resources/application-docker.properties` | Docker profile (bind address) |
| `.github/workflows/ci.yml` | CI: Maven verify, npm build, Docker image builds |
