# Docker Deployment

This project can be deployed with Docker Compose and reached directly from a browser through the Spring Boot app.

## What gets started

- `app`: Spring Boot backend with the built React frontend served on the same port
- `postgres`: PostgreSQL database
- `whatsapp-bridge`: optional local WhatsApp bridge used by the app

## Default URLs

- Website: `http://localhost:8080`
- Backend health check: `http://localhost:8080/api/health`
- WhatsApp bridge status: `http://localhost:3100/status`
- PostgreSQL from host: `localhost:5433`

## Start locally

```bash
docker compose up -d --build
```

Then open:

```text
http://localhost:8080
```

## Run on a server

1. Install Docker and Docker Compose.
2. Copy the project to the server.
3. Open the server firewall for your app port, usually `8080`.
4. Start the stack:

```bash
docker compose up -d --build
```

5. Open `http://YOUR_SERVER_IP:8080` in a browser.

## Persistent data

Docker named volumes keep these files after container restarts:

- PostgreSQL data
- uploaded files
- WhatsApp bridge session data

## Stop the stack

```bash
docker compose down
```

To also remove persistent volumes:

```bash
docker compose down -v
```
