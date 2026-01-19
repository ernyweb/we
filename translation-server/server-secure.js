#!/usr/bin/env node
/**
 * Secure Translation Server with HMAC Authentication
 * Supports EN, TR, RU, ES, FR with comprehensive dictionaries
 */

const express = require('express');
const crypto = require('crypto');
const cors = require('cors');
require('dotenv').config();

const app = express();
const PORT = process.env.PORT || 3000;

// Security Configuration
const SERVER_SECRET = process.env.SERVER_SECRET || 'GxKin3To18njIeQJlYzXuZI5zwf05pgE';
const API_KEYS = new Set([
  'eZSF1Eq2XZzecsD7zsA6rL1K',
]);

// Load comprehensive dictionaries
const translations = {
  'en-tr': require('./lang/en-tr.json'),
  'en-ru': require('./lang/en-ru.json'),
  'en-es': require('./lang/en-es.json'),
  'en-fr': require('./lang/en-fr.json'),
  'tr-en': require('./lang/tr-en.json'),
  'ru-en': require('./lang/ru-en.json'),
  'es-en': require('./lang/es-en.json'),
  'fr-en': require('./lang/fr-en.json'),
};

// Middleware
app.use(cors());
app.use(express.json());
app.use(express.urlencoded({ extended: true }));

// Request Logging Middleware
const requestHistory = [];
const MAX_HISTORY = 100;

app.use((req, res, next) => {
  const startTime = Date.now();
  const originalSend = res.send;
  
  res.send = function(data) {
    const duration = Date.now() - startTime;
    const logEntry = {
      timestamp: new Date().toISOString(),
      method: req.method,
      path: req.path,
      ip: req.ip || req.connection.remoteAddress,
      userAgent: req.headers['user-agent'],
      duration: `${duration}ms`,
      status: res.statusCode
    };
    
    // Add to history (keep last 100)
    requestHistory.unshift(logEntry);
    if (requestHistory.length > MAX_HISTORY) {
      requestHistory.pop();
    }
    
    // Console log with colors
    const statusColor = res.statusCode >= 400 ? '\x1b[31m' : '\x1b[32m';
    const resetColor = '\x1b[0m';
    console.log(
      `${statusColor}[${logEntry.timestamp}]${resetColor} ` +
      `${req.method} ${req.path} - ${statusColor}${res.statusCode}${resetColor} - ${duration}ms - ${req.ip}`
    );
    
    return originalSend.call(this, data);
  };
  
  next();
});

// HMAC Signature Verification
function verifySignature(data, signature, timestamp) {
  // Prevent replay attacks (5 min window)
  const now = Date.now();
  const requestTime = parseInt(timestamp);
  if (Math.abs(now - requestTime) > 300000) {
    return false;
  }

  const payload = `${data}|${timestamp}`;
  const expectedSig = crypto
    .createHmac('sha256', SERVER_SECRET)
    .update(payload)
    .digest('hex');
  
  return crypto.timingSafeEqual(
    Buffer.from(signature),
    Buffer.from(expectedSig)
  );
}

// Sign Response
function signResponse(data) {
  const timestamp = Date.now().toString();
  const payload = `${JSON.stringify(data)}|${timestamp}`;
  const signature = crypto
    .createHmac('sha256', SERVER_SECRET)
    .update(payload)
    .digest('hex');
  
  return {
    data,
    timestamp,
    signature,
  };
}

// API Key Middleware
const requireApiKey = (req, res, next) => {
  const apiKey = req.headers['x-api-key'] || req.query.api_key;
  
  if (!apiKey || !API_KEYS.has(apiKey)) {
    return res.status(401).json(signResponse({
      error: 'Unauthorized',
      message: 'Invalid API key'
    }));
  }
  
  next();
};

// Signature Middleware (for POST requests)
const requireSignature = (req, res, next) => {
  if (req.method === 'POST') {
    const signature = req.headers['x-signature'];
    const timestamp = req.headers['x-timestamp'];
    const data = JSON.stringify(req.body);
    
    if (!signature || !timestamp || !verifySignature(data, signature, timestamp)) {
      return res.status(403).json(signResponse({
        error: 'Forbidden',
        message: 'Invalid signature or expired request'
      }));
    }
  }
  
  next();
};

// Translation Function with Logging
const translationHistory = [];
const MAX_TRANSLATION_HISTORY = 50;

function translate(text, fromLang, toLang) {
  // Normalize language codes to lowercase
  const key = `${fromLang.toLowerCase()}-${toLang.toLowerCase()}`;
  const dict = translations[key];
  
  if (!dict) {
    return {
      success: false,
      error: `Translation pair ${key} not supported`
    };
  }
  
  // Normalize text
  const normalized = text.toLowerCase().trim();
  const words = normalized.split(/\s+/);
  const translated = words.map(word => {
    const clean = word.replace(/[.,!?;:]/g, '');
    return dict[clean] || word;
  });
  
  const result = {
    success: true,
    original: text,
    translated: translated.join(' '),
    from: fromLang,
    to: toLang,
    wordsTranslated: translated.filter((w, i) => w !== words[i].replace(/[.,!?;:]/g, '')).length,
    totalWords: words.length
  };
  
  // Log translation
  const logEntry = {
    timestamp: new Date().toISOString(),
    text: text.substring(0, 100), // Limit length
    from: fromLang,
    to: toLang,
    result: result.translated.substring(0, 100)
  };
  
  translationHistory.unshift(logEntry);
  if (translationHistory.length > MAX_TRANSLATION_HISTORY) {
    translationHistory.pop();
  }
  
  // Console log translation
  console.log(
    `\x1b[36m[TRANSLATE]\x1b[0m ${fromLang}→${toLang}: ` +
    `"${text.substring(0, 50)}" → "${result.translated.substring(0, 50)}"`
  );
  
  return result;
}

// Routes

// Health Check
app.get('/', (req, res) => {
  res.json(signResponse({
    service: 'Secure Translation Server',
    version: '2.0.0',
    status: 'running',
    security: 'HMAC-SHA256',
    languages: ['en', 'tr', 'ru', 'es', 'fr'],
    timestamp: new Date().toISOString()
  }));
});

// List supported languages
app.get('/languages', requireApiKey, (req, res) => {
  res.json(signResponse({
    languages: [
      { code: 'en', name: 'English', nativeName: 'English' },
      { code: 'tr', name: 'Turkish', nativeName: 'Türkçe' },
      { code: 'ru', name: 'Russian', nativeName: 'Русский' },
      { code: 'es', name: 'Spanish', nativeName: 'Español' },
      { code: 'fr', name: 'French', nativeName: 'Français' }
    ],
    pairs: Object.keys(translations)
  }));
});

// URL-based translation (GET)
app.get('/translate-:from-:text-to-:to', requireApiKey, (req, res) => {
  const { from, text, to } = req.params;
  const decodedText = text.replace(/\./g, ' ');
  
  const result = translate(decodedText, from, to);
  res.json(signResponse(result));
});

// JSON translation (POST - API key only, no signature required)
app.post('/translate', requireApiKey, (req, res) => {
  const { text, from, to, source, target } = req.body;
  
  // Support both from/to and source/target
  const sourceLanguage = (from || source || '').toUpperCase();
  const targetLanguage = (to || target || '').toUpperCase();
  
  if (!text || !sourceLanguage || !targetLanguage) {
    return res.status(400).json(signResponse({
      error: 'Bad Request',
      message: 'Missing required fields: text, source/from, target/to'
    }));
  }
  
  const result = translate(text, sourceLanguage, targetLanguage);
  res.json(signResponse(result));
});

// Download language pack (for offline use)
app.get('/download/:lang', requireApiKey, (req, res) => {
  const { lang } = req.params;
  const targetLang = req.query.target || 'en';
  const key = `${lang}-${targetLang}`;
  
  if (!translations[key]) {
    return res.status(404).json(signResponse({
      error: 'Not Found',
      message: `Language pack ${key} not available`
    }));
  }
  
  res.json(signResponse({
    pack: key,
    dictionary: translations[key],
    words: Object.keys(translations[key]).length,
    version: '1.0.0',
    downloadedAt: new Date().toISOString()
  }));
});

// Test endpoint (for CLI)
app.get('/test', requireApiKey, (req, res) => {
  const { text, from, to } = req.query;
  
  if (!text) {
    return res.status(400).json(signResponse({
      error: 'Missing text parameter',
      usage: '/test?text=hello&from=en&to=tr&api_key=YOUR_KEY'
    }));
  }
  
  const result = translate(text, from || 'en', to || 'tr');
  res.json(signResponse(result));
});

// History endpoint - Show recent translations and requests
app.get('/history', requireApiKey, (req, res) => {
  res.json(signResponse({
    requests: requestHistory.slice(0, 20),
    translations: translationHistory.slice(0, 20),
    totalRequests: requestHistory.length,
    totalTranslations: translationHistory.length
  }));
});

// Stats endpoint
app.get('/stats', requireApiKey, (req, res) => {
  const stats = {
    uptime: process.uptime(),
    totalRequests: requestHistory.length,
    totalTranslations: translationHistory.length,
    supportedLanguages: 5,
    languagePairs: Object.keys(translations).length,
    dictionarySize: Object.keys(translations).reduce((sum, key) => 
      sum + Object.keys(translations[key]).length, 0
    )
  };
  
  res.json(signResponse(stats));
});

// Error handling
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json(signResponse({
    error: 'Internal Server Error',
    message: process.env.NODE_ENV === 'development' ? err.message : 'Something went wrong'
  }));
});

// 404 handler
app.use((req, res) => {
  res.status(404).json(signResponse({
    error: 'Not Found',
    message: 'Endpoint not found',
    availableEndpoints: [
      'GET /',
      'GET /languages',
      'GET /translate-{from}-{text}-to-{to}',
      'POST /translate',
      'GET /download/:lang',
      'GET /test',
      'GET /history',
      'GET /stats'
    ]
  }));
});

// Start server
app.listen(PORT, '0.0.0.0', () => {
  console.log(`
╔═══════════════════════════════════════════════════════════╗
║     🔐 Secure Translation Server v2.0.0                  ║
╠═══════════════════════════════════════════════════════════╣
║  Status: ONLINE ✓                                         ║
║  Port: ${PORT}                                            ║
║  Security: HMAC-SHA256 + API Key Authentication          ║
║  Languages: EN, TR, RU, ES, FR                            ║
║  Total Dictionaries: ${Object.keys(translations).length}                                    ║
║  Logging: ✓ Request & Translation History                ║
╚═══════════════════════════════════════════════════════════╝

📡 Server ready at http://localhost:${PORT}
🔑 API Keys configured: ${API_KEYS.size}
📊 History endpoints: /history, /stats
  `);
});

module.exports = app;
