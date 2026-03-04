# E2E Tests (Sign up & Sign in)

End-to-end tests use **Playwright** and cover the auth flows.

## Prerequisites

1. **Backend** must be running on **port 8080**  
   - In IntelliJ: run `TalabatyApplication` (Spring Boot).  
   - Or from project root: `./mvnw spring-boot:run`

2. **Frontend** will be started automatically by Playwright if nothing is already on port 3000.  
   - Or run manually: `npm run dev` (port 3000).

3. **Install Playwright browsers** (once):

   ```bash
   cd talabaty-frontend
   npx playwright install chromium
   ```

## Run E2E tests

From `talabaty-frontend`:

```bash
npm run e2e
```

- **With UI**: `npm run e2e:ui`  
- **Headed (see browser)**: `npm run e2e:headed`

## If something fails in IntelliJ

- Ensure the **Spring Boot app** is running (port 8080).  
- Ensure **PostgreSQL** (or your DB) is up and the app connects.  
- Run the **frontend** in a terminal: `cd talabaty-frontend && npm run dev`, then run `npm run e2e` in another terminal so the app is on port 3000.  
- Check backend logs in IntelliJ for auth/signup errors (e.g. DB, validation).

## What the tests do

- **Sign up**: Open `/signup`, fill form (unique email), submit → expect redirect to app (select store or dashboard).  
- **Sign in**: Open `/login`, wrong password → error message; sign up then sign in with same user → lands in app.  
- **Navigation**: Links between login and signup pages work.
