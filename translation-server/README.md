# Translation Server

Self-hosted translation server with API key authentication and downloadable language packs.

## Features

- ✅ **4 Languages**: English, Turkish, Spanish, French
- ✅ **API Key Authentication**: Secure access control
- ✅ **URL-based Translation**: Simple GET requests
- ✅ **JSON API**: POST endpoint for apps
- ✅ **Downloadable Language Packs**: For offline use in apps
- ✅ **CORS Enabled**: Works with web and mobile apps

## Installation

```bash
cd translation-server
npm install
npm start
```

Server runs on `http://localhost:3000`

## API Endpoints

### 1. Health Check
```bash
GET /
```

### 2. Get Supported Languages
```bash
GET /languages
```

Response:
```json
{
  "languages": {
    "en": { "code": "en", "name": "English", "nativeName": "English" },
    "tr": { "code": "tr", "name": "Turkish", "nativeName": "Türkçe" },
    "es": { "code": "es", "name": "Spanish", "nativeName": "Español" },
    "fr": { "code": "fr", "name": "French", "nativeName": "Français" }
  },
  "supportedPairs": [
    { "from": "en", "to": "tr" },
    { "from": "tr", "to": "en" },
    { "from": "en", "to": "es" },
    { "from": "en", "to": "fr" }
  ]
}
```

### 3. URL-based Translation
```bash
GET /translate-{from}-{text}-to-{to}?api_key=YOUR_API_KEY
```

**Example:**
```bash
# Single word
curl "http://localhost:3000/translate-en-hello-to-tr?api_key=test-key-123"

# Multiple words (use dots)
curl "http://localhost:3000/translate-en-i.am.feeling.bad-to-tr?api_key=test-key-123"
```

Response:
```json
{
  "original": "i am feeling bad",
  "translated": "ben hissediyorum kötü",
  "from": "en",
  "to": "tr",
  "method": "url"
}
```

### 4. JSON POST Translation
```bash
POST /translate
Headers: { "X-API-Key": "YOUR_API_KEY" }
Body: {
  "text": "hello",
  "from": "en",
  "to": "tr"
}
```

**Example:**
```bash
curl -X POST http://localhost:3000/translate \
  -H "Content-Type: application/json" \
  -H "X-API-Key: test-key-123" \
  -d '{"text":"hello","from":"en","to":"tr"}'
```

Response:
```json
{
  "original": "hello",
  "translated": "merhaba",
  "from": "en",
  "to": "tr",
  "method": "post"
}
```

### 5. Download Language Pack
```bash
GET /download/{lang}?api_key=YOUR_API_KEY
```

**Example:**
```bash
# Download English language pack (all EN-* dictionaries)
curl "http://localhost:3000/download/en?api_key=test-key-123"
```

Response:
```json
{
  "language": "en",
  "dictionaries": {
    "en-tr": { "hello": "merhaba", ... },
    "en-es": { "hello": "hola", ... },
    "en-fr": { "hello": "bonjour", ... }
  },
  "wordCount": 180,
  "version": "1.0.0",
  "downloadedAt": "2026-01-11T10:30:00.000Z"
}
```

## API Keys

Default API keys (change these in production!):
- `translation-key-2026-secure-abc123`
- `test-key-123`
- `your-api-key-here`

Add your own keys in `server.js`:
```javascript
const VALID_API_KEYS = [
  'your-custom-api-key-here'
];
```

## VPS Deployment

### Option 1: PM2 (Recommended)
```bash
npm install -g pm2
pm2 start server.js --name translation-server
pm2 save
pm2 startup
```

### Option 2: systemd
Create `/etc/systemd/system/translation-server.service`:
```ini
[Unit]
Description=Translation Server
After=network.target

[Service]
Type=simple
User=your-user
WorkingDirectory=/path/to/translation-server
ExecStart=/usr/bin/node server.js
Restart=on-failure

[Install]
WantedBy=multi-user.target
```

Then:
```bash
sudo systemctl enable translation-server
sudo systemctl start translation-server
```

### Nginx Reverse Proxy
```nginx
server {
    listen 80;
    server_name your-domain.com;

    location / {
        proxy_pass http://localhost:3000;
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection 'upgrade';
        proxy_set_header Host $host;
        proxy_cache_bypass $http_upgrade;
    }
}
```

## Environment Variables

Create `.env` file:
```env
PORT=3000
NODE_ENV=production
```

## Usage in Android App

```kotlin
// Using your URL format
val url = "http://your-vps-ip:3000/translate-en-hello-to-tr?api_key=test-key-123"

// Using OkHttp
val client = OkHttpClient()
val request = Request.Builder()
    .url(url)
    .build()

client.newCall(request).enqueue(object : Callback {
    override fun onResponse(call: Call, response: Response) {
        val json = JSONObject(response.body?.string())
        val translated = json.getString("translated")
        // Use translated text
    }
})
```

## Supported Translation Pairs

- English ↔ Turkish
- English → Spanish
- English → French

## License

MIT
