# Development workflow

## Avoid restarting Docker for every UI change

**Use local frontend + backend (Docker or local).**

1. **Backend** (choose one):
   - **Option A – Docker**: `docker compose up` → backend + Postgres. API at **http://localhost:8080**.
   - **Option B – Local**: Run the Spring Boot app from your IDE (e.g. TalabatyApplication). API at **http://localhost:8080**.

2. **Frontend (with hot reload)**  
   From project root:
   ```bash
   cd talabaty-frontend && npm run dev
   ```
   - App: **http://localhost:3000**
   - Edits to React/TS/CSS trigger **instant reload**; no Docker restart.

3. **API calls**  
   The Vite dev server proxies `/api` to `http://localhost:8080`, so the frontend uses `/api` and talks to your backend. No CORS issues.

---

## YouCan integration and ports

- **YouCan talks only to the backend (port 8080).**
  - OAuth callback: `http://localhost:8080/api/youcan/oauth/callback`
  - Webhooks (if any): also to the backend.
- **Frontend port (3000, 3030, etc.) does not affect YouCan.** YouCan never calls the frontend.

When you use the **frontend on 3000** (e.g. `npm run dev`):

- Run the backend with the **dev** profile so that after YouCan OAuth it redirects to the dev server:
  ```bash
  SPRING_PROFILES_ACTIVE=dev java -jar app.jar
  ```
  or in IDE: run configuration with `-Dspring.profiles.active=dev`.
- This sets `app.frontend.url=http://localhost:3000`, so after connecting YouCan you land back on the app at 3000.

When you use **only Docker** (frontend built into the app, single port 8080):

- No profile needed; `app.frontend.url` stays `http://localhost:8080` (or your production URL).
- YouCan continues to work; UI changes require rebuilding the image and restarting the container.

---

## Summary

| Goal                         | Backend        | Frontend              | YouCan |
|-----------------------------|----------------|------------------------|--------|
| UI changes without restart  | Docker or IDE  | `npm run dev` (3000)   | Use `dev` profile so redirect goes to 3000 |
| Production-like (one box)   | Docker         | Served from 8080       | Works; redirect to 8080 |

Running the app on port **3000** (or 3030) does **not** break the YouCan integration; only the backend port **8080** matters for YouCan. Use the **dev** profile when the UI is on 3000 so the post-OAuth redirect is correct.
