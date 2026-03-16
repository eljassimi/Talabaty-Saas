# Free WhatsApp setup (whatsapp-web.js bridge)

Use this option to send WhatsApp messages **without paying** for Twilio. Talabaty talks to a small Node.js bridge that uses your own WhatsApp account (via WhatsApp Web).

**Trade-offs:**
- **Free** – no per-message cost.
- **Unofficial** – uses [whatsapp-web.js](https://wwebjs.dev/), which automates WhatsApp Web. Your account could theoretically be restricted by WhatsApp if they detect automation; use a secondary number for production if you’re worried.
- **You must run the bridge** – a separate Node.js process next to your Talabaty backend.
- **One-time QR login** – scan a QR code once; the session is saved so you don’t need to scan again after restarts.

---

## 1. Install and run the bridge

From the project root:

```bash
cd whatsapp-bridge
npm install
npm start
```

The first time (or when not linked), open Talabaty → **Automations** to see the **QR code on the website** in the "Link WhatsApp (free bridge)" section. Open WhatsApp on your phone → **Settings → Linked devices → Link a device** and scan that QR code. After that, the bridge keeps the session in `.wwebjs_auth` and you usually won’t need to scan again unless you log out or clear the folder.

The bridge listens on **http://localhost:3100** by default. You can set `PORT=3100` (or another port) in the environment if needed.

---

## 2. Point Talabaty to the bridge

In your Talabaty backend config (e.g. `application.properties` or environment variables), set:

```properties
whatsapp.local.url=http://localhost:3100
```

Do **not** set the Twilio options if you’re only using the free bridge. Talabaty will use the bridge when `whatsapp.local.url` is set.

If the bridge runs on another machine, use that host and port, e.g. `http://192.168.1.10:3100`.

---

## 3. Restart Talabaty backend

Restart the Talabaty application so it loads `whatsapp.local.url`. After that, automation (order confirmed/delivered) and **Send promotion to all customers** will use the bridge.

---

## 4. Automations and promotions

- **Automations** – In Talabaty go to **Automations**, enable WhatsApp automation and set the templates. When an order is set to Confirmed or Delivered, the message is sent via the bridge.
- **Promotions** – In **Automations**, use the **“Send promotion to all customers”** section: write your message and click **Send to all customers**. Talabaty sends it to every unique customer phone from that store’s orders (with a short delay between each to reduce rate limits).

---

## Bridge API (for reference)

The bridge exposes:

| Endpoint       | Method | Body | Description        |
|----------------|--------|------|--------------------|
| `/send`        | POST   | `{ "to": "+212612345678", "message": "Text" }` | Send one message. |
| `/send-bulk`   | POST   | `{ "recipients": ["+212..."], "message": "Text" }` | Send to many (with internal delay). |
| `/status`      | GET    | -    | Returns `{ "ready": true/false }`. |

Phone numbers should include country code (e.g. `+212` for Morocco). The bridge converts them to WhatsApp chat IDs.

---

## Troubleshooting

| Issue | What to do |
|-------|------------|
| “WhatsApp not ready” in bridge response | Scan the QR code in the bridge terminal. Wait until you see “WhatsApp client is ready.” |
| Talabaty says “WhatsApp is not configured” | Set `whatsapp.local.url` and restart the backend. Ensure the bridge is running (e.g. `curl http://localhost:3100/status`). |
| Bridge crashes or QR keeps appearing | Delete the `.wwebjs_auth` folder and run the bridge again, then scan the QR once more. |
| Messages not received | Confirm the contact has WhatsApp and the number is correct (with country code). Check bridge logs for errors. |

---

## Running the bridge in production

- Run the bridge with a process manager (e.g. **pm2**): `pm2 start server.js --name whatsapp-bridge`.
- Keep the same machine and path for `.wwebjs_auth` so the session persists.
- If Talabaty runs in Docker and the bridge on the host, use the host URL (e.g. `http://host.docker.internal:3100` on Docker Desktop) or the server’s LAN IP instead of `localhost`.
