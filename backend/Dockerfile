FROM node:20-bookworm-slim

# yt-dlp resolves YouTube metadata; ffmpeg converts the selected audio to MP3.
RUN apt-get update \
    && apt-get install -y --no-install-recommends ffmpeg python3-pip \
    && pip3 install --no-cache-dir --break-system-packages yt-dlp \
    && rm -rf /var/lib/apt/lists/*

WORKDIR /app

COPY package*.json ./
RUN npm ci --omit=dev

COPY . ./

ENV NODE_ENV=production
ENV PORT=8080

EXPOSE 8080

CMD ["node", "index.js"]
