# Configure Twilio for WhatsApp automation

Talabaty sends WhatsApp messages to customers when an order is set to **Confirmed** or **Delivered**. This uses Twilio’s WhatsApp API. Follow these steps to configure it.

---

## 1. Create a Twilio account

1. Go to [https://www.twilio.com/try-twilio](https://www.twilio.com/try-twilio) and sign up.
2. Verify your email and phone if asked.

---

## 2. Get your Account SID and Auth Token

1. Log in to the [Twilio Console](https://console.twilio.com).
2. On the dashboard you’ll see:
   - **Account SID** (starts with `AC`)
   - **Auth Token** (click “Show” to reveal it)
3. Keep these secret; don’t commit them to git.

---

## 3. Enable WhatsApp: Sandbox (testing) or Production

### Option A: WhatsApp Sandbox (quickest for testing)

1. In Twilio Console go to **Messaging** → **Try it out** → **Send a WhatsApp message** (or **Develop** → **Messaging** → **Try it out** → **WhatsApp**).
2. Open the **WhatsApp Sandbox** section.
3. You’ll see a **Sandbox number** (e.g. `+1 415 523 8886`) and a **Join code** (e.g. `join <word>-<word>`).
4. On your personal WhatsApp, send that join code to the sandbox number. After that, the sandbox can send you messages.
5. For testing with real customers, each recipient must send the same join code to the sandbox number once.

**Sandbox “From” number:** use the sandbox number exactly as shown (e.g. `+14155238886`), with country code and no spaces.

### Option B: WhatsApp Business API (production)

1. In Twilio Console go to **Messaging** → **WhatsApp** → **Senders** (or **Phone Numbers** → **WhatsApp**).
2. Request a **Twilio WhatsApp sender** or link your **Meta WhatsApp Business** number. Follow Twilio’s flow (approval can take a short time).
3. Once approved, you’ll have a WhatsApp-capable number. Use that number as the “From” number (E.164, e.g. `+14155551234`).

---

## 4. Add credentials to Talabaty

Set these three values. You can use **application.properties** (for local/dev) or **environment variables** (recommended for production).

### Using application.properties (local / dev)

Edit `src/main/resources/application.properties` and set:

```properties
# WhatsApp (Twilio)
twilio.account-sid=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
twilio.auth-token=your-auth-token-here
twilio.whatsapp.from=+14155238886
```

- **twilio.account-sid** — Your Twilio Account SID (starts with `AC`).
- **twilio.auth-token** — Your Twilio Auth Token.
- **twilio.whatsapp.from** — Your WhatsApp sender number in E.164 (e.g. `+14155238886` for sandbox, or your Twilio WhatsApp Business number). No spaces; include country code.

Do **not** commit real credentials. Prefer environment variables or a local override file (e.g. `application-local.properties` in `.gitignore`).

### Using environment variables (production / Docker)

Set:

- `TWILIO_ACCOUNT_SID`
- `TWILIO_AUTH_TOKEN`
- `TWILIO_WHATSAPP_FROM`

Spring Boot maps them to `twilio.account-sid`, `twilio.auth-token`, and `twilio.whatsapp.from`. Example in Docker or a `.env`:

```bash
export TWILIO_ACCOUNT_SID=ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
export TWILIO_AUTH_TOKEN=your-auth-token
export TWILIO_WHATSAPP_FROM=+14155238886
```

Or in `docker-compose.yml`:

```yaml
environment:
  TWILIO_ACCOUNT_SID: ${TWILIO_ACCOUNT_SID}
  TWILIO_AUTH_TOKEN: ${TWILIO_AUTH_TOKEN}
  TWILIO_WHATSAPP_FROM: ${TWILIO_WHATSAPP_FROM}
```

---

## 5. Restart the backend

After changing config, restart the Talabaty backend so it loads the new Twilio settings.

---

## 6. Enable automation in Talabaty

1. In Talabaty, go to **Automations** (sidebar).
2. Select the store you want.
3. Turn **Enable WhatsApp automation** on.
4. Set the two message templates (e.g. for “Order confirmed” and “Order delivered”). You can use placeholders: `{{customerName}}`, `{{orderId}}`, `{{trackingNumber}}`, `{{totalAmount}}`, `{{currency}}`, `{{city}}`.
5. Click **Save**.

When an order’s status is updated to **Confirmed** or **Delivered**, Talabaty will send the corresponding template to the customer’s phone number via Twilio.

---

## Phone number format

- **From:** E.164 (e.g. `+14155238886`). The app adds the `whatsapp:` prefix when calling Twilio.
- **To (customer):** Stored in the order as `customerPhone`. Twilio expects E.164. The app adds `+` if missing and normalizes a leading zero (e.g. `0612345678` in Morocco → `+212612345678`). Ensure your orders have phone numbers with country code when possible (e.g. `+212612345678` for Morocco).

---

## Troubleshooting

| Issue | What to check |
|-------|----------------|
| “Twilio is not configured yet” in Automations | All three properties are set and the backend was restarted. |
| Messages not sent | Recipient joined the sandbox (if using Sandbox). Customer phone is in E.164. Check backend logs for Twilio errors. |
| 404 / “Sender not found” | Wrong or inactive WhatsApp “from” number; confirm the number in Twilio Console. |
| 21219 (invalid “To”) | Customer number format; use country code (e.g. `+212...` for Morocco). |

For more: [Twilio WhatsApp API](https://www.twilio.com/docs/whatsapp), [Twilio Console](https://console.twilio.com).
