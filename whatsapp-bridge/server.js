/**
 * Talabaty WhatsApp Bridge (free) - uses whatsapp-web.js.
 * Run: npm install && npm start
 * Then set whatsapp.local.url=http://localhost:3100 in Talabaty backend.
 *
 * API:
 *   POST /send   Body: { "to": "+212612345678", "message": "Hello" }
 *   POST /send-bulk  Body: { "recipients": ["+212...", "+212..."], "message": "Hello" }
 */
const express = require('express');
const path = require('path');
const fs = require('fs');
const { Client, LocalAuth } = require('whatsapp-web.js');
const qrcode = require('qrcode');

// Base directory for auth; each store/session uses its own subfolder so
// multiple stores can have different WhatsApp accounts.
const AUTH_DIR_BASE = path.join(process.cwd(), '.wwebjs_auth');

function getAuthDir(sessionId) {
  const sid = sessionId || 'default';
  return path.join(AUTH_DIR_BASE, sid);
}

function removeStaleBrowserLocks(dir) {
  try {
    if (!fs.existsSync(dir)) return;
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const e of entries) {
      const full = path.join(dir, e.name);
      if (e.name === 'SingletonLock') {
        fs.unlinkSync(full);
        console.log('Removed stale browser lock');
        return;
      }
      if (e.isDirectory() && !e.name.startsWith('.')) {
        removeStaleBrowserLocks(full);
      }
    }
  } catch (err) {
    // ignore
  }
}

function clearSessionAfterLogout(sessionId) {
  try {
    const dir = getAuthDir(sessionId);
    if (!fs.existsSync(dir)) return;
    const entries = fs.readdirSync(dir, { withFileTypes: true });
    for (const e of entries) {
      const full = path.join(dir, e.name);
      fs.rmSync(full, { recursive: true, force: true });
    }
    console.log('Cleared session after logout – new QR will be generated');
  } catch (err) {
    console.error('Could not clear session:', err.message);
  }
}

const PORT = process.env.PORT || 3100;
const app = express();
app.use(express.json());

let client = null;
let currentSessionId = 'default';
let ready = false;
let lastQrDataUrl = null;
let initializing = false;
let initError = null;
let restarting = false;
/** Set when we restarted after logout/disconnect so the UI can show "You were logged out" and a new QR */
let loggedOutReason = null;
let stateCheckFailures = 0;
let nonConnectedChecks = 0;
let relinkWatchdog = null;
let lastRelinkAt = 0;
let lastKnownState = 'UNKNOWN';

function triggerRelink(reason = 'logged_out') {
  const now = Date.now();
  // Debounce relink attempts to avoid restart loops.
  if (now - lastRelinkAt < 5000) return;
  lastRelinkAt = now;
  if (restarting) return;
  loggedOutReason = reason;
  ready = false;
  initializing = true;
  lastQrDataUrl = null;
  initError = null;
  const c = client;
  client = null;
  restarting = true;
  stateCheckFailures = 0;
  nonConnectedChecks = 0;
  if (relinkWatchdog) clearTimeout(relinkWatchdog);
  // Fallback: if destroy/restart hangs, force a new client so QR appears.
  relinkWatchdog = setTimeout(() => {
    if (!restarting) return;
    console.log('Relink watchdog fired; forcing bridge restart.');
    clearSessionAfterLogout(currentSessionId);
    startClient(currentSessionId);
  }, 8000);
  const restart = () => {
    setTimeout(() => {
      clearSessionAfterLogout(currentSessionId);
      startClient(currentSessionId);
    }, 1500);
  };
  if (c) {
    c.destroy().then(restart).catch(restart);
  } else {
    restart();
  }
}

function startClient(sessionIdArg) {
  const sessionId = sessionIdArg || currentSessionId || 'default';
  currentSessionId = sessionId;
  if (relinkWatchdog) {
    clearTimeout(relinkWatchdog);
    relinkWatchdog = null;
  }
  restarting = false;
  initializing = true;
  initError = null;
  lastQrDataUrl = null;
  removeStaleBrowserLocks(getAuthDir(sessionId));
  const puppeteerOpts = {
    headless: true,
    args: [
      '--no-sandbox',
      '--disable-setuid-sandbox',
      '--disable-dev-shm-usage',
      '--disable-gpu',
      '--disable-software-rasterizer',
      '--disable-extensions',
      '--no-zygote',
      '--no-first-run',
      '--disable-background-networking',
      '--disable-default-apps',
    ],
  };
  if (process.env.PUPPETEER_EXECUTABLE_PATH) {
    puppeteerOpts.executablePath = process.env.PUPPETEER_EXECUTABLE_PATH;
  }
  client = new Client({
    authStrategy: new LocalAuth({ dataPath: path.join('.wwebjs_auth', sessionId) }),
    puppeteer: puppeteerOpts,
  });

  client.on('qr', (qr) => {
    initializing = false;
    initError = null;
    console.log('QR code ready – scan it on the website (Automations → Link your WhatsApp)');
    qrcode.toDataURL(qr, { margin: 2, width: 280 }).then((dataUrl) => {
      lastQrDataUrl = dataUrl;
    }).catch((err) => {
      console.error('QR toDataURL failed:', err);
    });
  });

  client.on('ready', () => {
    initializing = false;
    initError = null;
    loggedOutReason = null;
    stateCheckFailures = 0;
    nonConnectedChecks = 0;
    lastKnownState = 'CONNECTED';
    console.log('WhatsApp client is ready.');
    ready = true;
    lastQrDataUrl = null;
  });

  client.on('change_state', (state) => {
    const s = String(state || '').toUpperCase();
    lastKnownState = s || 'UNKNOWN';
    console.log('WhatsApp state changed:', s || '(empty)');
    // Primary logout detector: WhatsApp explicitly reports unpaired.
    if (s === 'UNPAIRED' || s === 'UNPAIRED_IDLE') {
      console.log(`Detected logout state via change_state (${s}); triggering relink.`);
      triggerRelink('logged_out');
    }
  });

  client.on('auth_failure', (msg) => {
    initializing = false;
    console.error('Auth failure:', msg);
    triggerRelink('logged_out');
  });

  client.on('disconnected', (reason) => {
    console.log('Disconnected:', reason);
    triggerRelink('logged_out');
  });

  client.initialize().catch((err) => {
    initializing = false;
    initError = err && (err.message || String(err));
    console.error('Initialize failed:', err);
  });
}

function getClient(sessionId) {
  const sid = sessionId || 'default';
  if (currentSessionId !== sid) {
    currentSessionId = sid;
    ready = false;
    lastQrDataUrl = null;
    initializing = true;
    initError = null;
    if (client) {
      try {
        client.destroy();
      } catch (e) {
        // ignore
      }
      client = null;
    }
  }
  if (!client && !restarting) {
    startClient(currentSessionId);
  }
  return client;
}

// Normalize phone to chat id: 212612345678@c.us (no +)
function toChatId(phone) {
  const p = String(phone).trim().replace(/^\+/, '').replace(/\D/g, '');
  return p ? `${p}@c.us` : null;
}

async function sendOne(sessionId, toPhone, message) {
  const c = getClient(sessionId);
  if (!c || !ready) {
    return { ok: false, error: 'WhatsApp not ready. Link your number in Automations first.' };
  }
  const chatId = toChatId(toPhone);
  if (!chatId) {
    return { ok: false, error: 'Invalid phone number' };
  }
  try {
    await c.sendMessage(chatId, message);
    return { ok: true };
  } catch (err) {
    const msg = (err && (err.message || String(err))) || 'Send failed';
    // If the session has been invalidated, force relink so the UI shows a new QR.
    const lower = String(msg).toLowerCase();
    if (lower.includes('session') || lower.includes('not logged') || lower.includes('wid error')) {
      triggerRelink('logged_out');
      return { ok: false, error: 'WhatsApp session expired. Please link your device again in Automations.' };
    }
    return { ok: false, error: msg };
  }
}

app.post('/send', async (req, res) => {
  const { to, message, sessionId } = req.body || {};
  const sid = req.query.sessionId || sessionId || 'default';
  if (!to || !message) {
    return res.status(400).json({ error: 'Missing "to" or "message"' });
  }
  const result = await sendOne(sid, to, message);
  if (result.ok) {
    return res.status(200).json({ success: true });
  }
  return res.status(500).json({ error: result.error });
});

app.post('/send-bulk', async (req, res) => {
  const { recipients, message } = req.body || {};
  if (!Array.isArray(recipients) || !message) {
    return res.status(400).json({ error: 'Missing "recipients" array or "message"' });
  }
  const results = { sent: 0, failed: 0 };
  for (const to of recipients) {
    const r = await sendOne(to, message);
    if (r.ok) results.sent++;
    else results.failed++;
    await new Promise((r) => setTimeout(r, 1500));
  }
  return res.status(200).json(results);
});

app.get('/status', (req, res) => {
  res.json({ ready });
});

// For website: get current QR to display so admins can link WhatsApp in the browser
// Session is saved (one-time link); QR shows again only after user logs out on phone.
app.get('/qr', async (req, res) => {
  const sid = req.query.sessionId || 'default';
  getClient(sid);
  // If we think we're ready, verify the live state.
  // IMPORTANT: only force relink on explicit unpaired states, not transitional states,
  // otherwise a second scan may never reach "ready".
  if (ready && client && !restarting && !initializing) {
    try {
      const state = await client.getState();
      const s = String(state || '').toUpperCase();
      if (s) lastKnownState = s;
      const shouldRelink = s === 'UNPAIRED' || s === 'UNPAIRED_IDLE';
      if (shouldRelink) {
        console.log(`Detected unpaired state while ready (${s}); triggering relink.`);
        triggerRelink('logged_out');
      } else {
        const connectedLike = s === 'CONNECTED' || s === 'OPENING' || s === 'PAIRING';
        if (connectedLike) {
          nonConnectedChecks = 0;
          stateCheckFailures = 0;
        } else {
          // Fallback detector for cases where logout does not emit "UNPAIRED".
          nonConnectedChecks += 1;
          if (nonConnectedChecks >= 6) {
            console.log(`Detected prolonged non-connected state (${s || lastKnownState}); triggering relink.`);
            triggerRelink('logged_out');
          }
        }
      }
    } catch (e) {
      // Tolerate transient read errors, but if persistent while "ready", force relink.
      stateCheckFailures += 1;
      if (stateCheckFailures >= 5) {
        console.log('Could not read WhatsApp state repeatedly while ready; triggering relink.');
        triggerRelink('logged_out');
      } else {
        console.log('Could not read WhatsApp state; keeping current session.');
      }
    }
  }

  if (ready) return res.json({ ready: true });
  if (lastQrDataUrl) {
    const body = { ready: false, qr: lastQrDataUrl, initializing: false };
    if (loggedOutReason) body.reason = loggedOutReason;
    return res.json(body);
  }
  if (initError) return res.json({ ready: false, error: initError, initializing: false });
  const body = { ready: false, initializing: !!initializing };
  if (loggedOutReason) body.reason = loggedOutReason;
  return res.json(body);
});

getClient();

// Keep process alive on transient puppeteer/whatsapp-web.js runtime errors.
process.on('uncaughtException', (err) => {
  console.error('Bridge uncaughtException:', err && err.message ? err.message : err);
});
process.on('unhandledRejection', (reason) => {
  console.error('Bridge unhandledRejection:', reason);
});

app.listen(PORT, () => {
  console.log(`Talabaty WhatsApp bridge running at http://localhost:${PORT}`);
  console.log('POST /send with { "to": "+212...", "message": "..." }');
});
