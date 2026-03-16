# WhatsApp bridge for Talabaty - runs alongside the main app so users can link WhatsApp in the UI
FROM node:18-bookworm-slim

WORKDIR /app

# Install deps for Puppeteer/Chromium
RUN apt-get update && apt-get install -y --no-install-recommends \
    chromium \
    fonts-liberation \
    && rm -rf /var/lib/apt/lists/*

ENV PUPPETEER_SKIP_CHROMIUM_DOWNLOAD=true
ENV PUPPETEER_EXECUTABLE_PATH=/usr/bin/chromium

COPY package*.json ./
RUN npm install --omit=dev

COPY server.js ./

EXPOSE 3100

VOLUME /app/.wwebjs_auth

CMD ["node", "server.js"]
