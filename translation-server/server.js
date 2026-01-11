const express = require('express');
const cors = require('cors');
const dictionaries = require('./dictionaries');

const app = express();
const PORT = process.env.PORT || 3000;

// Valid API keys (generate your own)
const VALID_API_KEYS = [
  'translation-key-2026-secure-abc123',
  'test-key-123',
  'your-api-key-here'
];

// Middleware
app.use(cors());
app.use(express.json());

// API Key validation middleware
const validateApiKey = (req, res, next) => {
  const apiKey = req.headers['x-api-key'] || req.query.api_key;
  
  if (!apiKey) {
    return res.status(401).json({ 
      error: 'API key required',
      message: 'Please provide API key in header (X-API-Key) or query parameter (api_key)'
    });
  }
  
  if (!VALID_API_KEYS.includes(apiKey)) {
    return res.status(403).json({ 
      error: 'Invalid API key',
      message: 'The provided API key is not valid'
    });
  }
  
  next();
};

// Translation function
function translate(text, sourceLang, targetLang) {
  const dictionaryKey = `${sourceLang}-${targetLang}`;
  const dictionary = dictionaries[dictionaryKey];
  
  if (!dictionary) {
    throw new Error(`Translation pair ${sourceLang}-${targetLang} not supported`);
  }
  
  // Convert to lowercase for matching
  const lowerText = text.toLowerCase().trim();
  
  // Direct match
  if (dictionary[lowerText]) {
    return dictionary[lowerText];
  }
  
  // Word-by-word translation
  const words = lowerText.split(/\s+/);
  const translatedWords = words.map(word => {
    // Remove punctuation for matching
    const cleanWord = word.replace(/[.,!?;:]/g, '');
    return dictionary[cleanWord] || word;
  });
  
  return translatedWords.join(' ');
}

// Routes

// Health check
app.get('/', (req, res) => {
  res.json({
    service: 'Translation Server',
    version: '1.0.0',
    status: 'running',
    endpoints: {
      translate: '/translate',
      urlTranslate: '/translate-{from}-{text}-to-{to}',
      languages: '/languages',
      download: '/download/{lang}'
    }
  });
});

// Get supported languages
app.get('/languages', (req, res) => {
  const languages = {
    'en': { code: 'en', name: 'English', nativeName: 'English' },
    'tr': { code: 'tr', name: 'Turkish', nativeName: 'Türkçe' },
    'es': { code: 'es', name: 'Spanish', nativeName: 'Español' },
    'fr': { code: 'fr', name: 'French', nativeName: 'Français' }
  };
  
  const supportedPairs = Object.keys(dictionaries).map(pair => {
    const [from, to] = pair.split('-');
    return { from, to };
  });
  
  res.json({
    languages,
    supportedPairs,
    totalPairs: supportedPairs.length
  });
});

// URL-based translation (your format)
// Example: /translate-en-hello.how.are.you-to-tr
app.get('/translate-:from-:text-to-:to', validateApiKey, (req, res) => {
  try {
    const { from, to, text } = req.params;
    
    // Replace dots with spaces
    const cleanText = text.replace(/\./g, ' ');
    
    const translatedText = translate(cleanText, from.toLowerCase(), to.toLowerCase());
    
    res.json({
      original: cleanText,
      translated: translatedText,
      from: from.toLowerCase(),
      to: to.toLowerCase(),
      method: 'url'
    });
  } catch (error) {
    res.status(400).json({
      error: error.message,
      original: req.params.text
    });
  }
});

// POST translation endpoint (JSON)
app.post('/translate', validateApiKey, (req, res) => {
  try {
    const { text, from, to } = req.body;
    
    if (!text || !from || !to) {
      return res.status(400).json({
        error: 'Missing required fields',
        required: ['text', 'from', 'to']
      });
    }
    
    const translatedText = translate(text, from.toLowerCase(), to.toLowerCase());
    
    res.json({
      original: text,
      translated: translatedText,
      from: from.toLowerCase(),
      to: to.toLowerCase(),
      method: 'post'
    });
  } catch (error) {
    res.status(400).json({
      error: error.message
    });
  }
});

// Download language pack (for offline use)
app.get('/download/:lang', validateApiKey, (req, res) => {
  const { lang } = req.params;
  
  // Find all dictionaries involving this language
  const relevantDicts = {};
  
  Object.keys(dictionaries).forEach(key => {
    if (key.startsWith(lang + '-') || key.endsWith('-' + lang)) {
      relevantDicts[key] = dictionaries[key];
    }
  });
  
  if (Object.keys(relevantDicts).length === 0) {
    return res.status(404).json({
      error: 'Language not found',
      language: lang
    });
  }
  
  res.json({
    language: lang,
    dictionaries: relevantDicts,
    wordCount: Object.values(relevantDicts).reduce((sum, dict) => sum + Object.keys(dict).length, 0),
    version: '1.0.0',
    downloadedAt: new Date().toISOString()
  });
});

// Error handling
app.use((err, req, res, next) => {
  console.error(err.stack);
  res.status(500).json({
    error: 'Internal server error',
    message: err.message
  });
});

// Start server
app.listen(PORT, () => {
  console.log(`🚀 Translation Server running on port ${PORT}`);
  console.log(`📚 Supported languages: EN, TR, ES, FR`);
  console.log(`🔑 API Keys configured: ${VALID_API_KEYS.length}`);
  console.log(`\n📝 Example usage:`);
  console.log(`   GET  http://localhost:${PORT}/translate-en-hello-to-tr?api_key=test-key-123`);
  console.log(`   POST http://localhost:${PORT}/translate`);
  console.log(`        Headers: { "X-API-Key": "test-key-123" }`);
  console.log(`        Body: { "text": "hello", "from": "en", "to": "tr" }`);
  console.log(`   GET  http://localhost:${PORT}/download/en?api_key=test-key-123`);
});

module.exports = app;
