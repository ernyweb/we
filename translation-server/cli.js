#!/usr/bin/env node
/**
 * Translation Server CLI Test Tool
 * Test translations interactively with HMAC signing
 */

const readline = require('readline');
const crypto = require('crypto');
const https = require('https');
const http = require('http');

// Configuration
const SERVER_URL = process.env.SERVER_URL || 'http://localhost:3000';
const API_KEY = process.env.API_KEY || 'translation-key-2026-secure-abc123';
const SERVER_SECRET = process.env.SERVER_SECRET || 'translation-server-secret-key-2026-secure';

const rl = readline.createInterface({
  input: process.stdin,
  output: process.stdout
});

// Colors for terminal
const colors = {
  reset: '\x1b[0m',
  bright: '\x1b[1m',
  green: '\x1b[32m',
  yellow: '\x1b[33m',
  blue: '\x1b[34m',
  cyan: '\x1b[36m',
  red: '\x1b[31m',
  magenta: '\x1b[35m',
};

function colorize(text, color) {
  return `${colors[color]}${text}${colors.reset}`;
}

// Sign request
function signRequest(data) {
  const timestamp = Date.now().toString();
  const payload = `${JSON.stringify(data)}|${timestamp}`;
  const signature = crypto
    .createHmac('sha256', SERVER_SECRET)
    .update(payload)
    .digest('hex');
  
  return { signature, timestamp };
}

// Verify response signature
function verifyResponse(response) {
  const { data, timestamp, signature } = response;
  const payload = `${JSON.stringify(data)}|${timestamp}`;
  const expectedSig = crypto
    .createHmac('sha256', SERVER_SECRET)
    .update(payload)
    .digest('hex');
  
  return signature === expectedSig;
}

// Make HTTP request
function makeRequest(endpoint, method = 'GET', body = null) {
  return new Promise((resolve, reject) => {
    const url = new URL(endpoint, SERVER_URL);
    url.searchParams.append('api_key', API_KEY);
    
    const client = url.protocol === 'https:' ? https : http;
    const options = {
      method,
      headers: {
        'Content-Type': 'application/json',
        'X-API-Key': API_KEY,
      }
    };
    
    if (body && method === 'POST') {
      const { signature, timestamp } = signRequest(body);
      options.headers['X-Signature'] = signature;
      options.headers['X-Timestamp'] = timestamp;
    }
    
    const req = client.request(url, options, (res) => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(data);
          resolve(parsed);
        } catch (e) {
          resolve({ data, raw: true });
        }
      });
    });
    
    req.on('error', reject);
    if (body) req.write(JSON.stringify(body));
    req.end();
  });
}

// Display banner
function showBanner() {
  console.log(colorize('\n╔═══════════════════════════════════════════════════════════╗', 'cyan'));
  console.log(colorize('║     🔐 Translation Server CLI Test Tool                  ║', 'cyan'));
  console.log(colorize('╠═══════════════════════════════════════════════════════════╣', 'cyan'));
  console.log(colorize(`║  Server: ${SERVER_URL.padEnd(45)} ║`, 'cyan'));
  console.log(colorize(`║  Security: HMAC-SHA256 ✓${' '.repeat(34)}║`, 'cyan'));
  console.log(colorize('╚═══════════════════════════════════════════════════════════╝\n', 'cyan'));
}

// Show help
function showHelp() {
  console.log(colorize('\nAvailable Commands:', 'bright'));
  console.log(colorize('  translate <text> <from> <to>  ', 'green') + '- Translate text');
  console.log(colorize('  languages                     ', 'green') + '- List supported languages');
  console.log(colorize('  download <lang> <target>      ', 'green') + '- Download language pack');
  console.log(colorize('  test <text> <from> <to>       ', 'green') + '- Quick test translation');
  console.log(colorize('  health                        ', 'green') + '- Server health check');
  console.log(colorize('  help                          ', 'green') + '- Show this help');
  console.log(colorize('  exit                          ', 'green') + '- Exit CLI\n');
  
  console.log(colorize('Examples:', 'yellow'));
  console.log('  translate hello en tr');
  console.log('  translate "how are you" en ru');
  console.log('  download en tr');
  console.log('  test goodbye en es\n');
}

// Handle translate command
async function handleTranslate(text, from, to) {
  try {
    console.log(colorize('\n⏳ Translating...', 'yellow'));
    
    const body = { text, from, to };
    const response = await makeRequest('/translate', 'POST', body);
    
    if (!verifyResponse(response)) {
      console.log(colorize('❌ Warning: Response signature invalid!', 'red'));
    }
    
    const { data } = response;
    
    if (data.success) {
      console.log(colorize('\n✓ Translation Successful:', 'green'));
      console.log(colorize('  Original:  ', 'cyan') + data.original);
      console.log(colorize('  Translated:', 'cyan') + data.translated);
      console.log(colorize('  Direction: ', 'cyan') + `${data.from} → ${data.to}`);
      console.log(colorize('  Coverage:  ', 'cyan') + `${data.wordsTranslated}/${data.totalWords} words`);
      console.log(colorize('  Accuracy:  ', 'cyan') + `${Math.round(data.wordsTranslated / data.totalWords * 100)}%\n`);
    } else {
      console.log(colorize('❌ Translation failed:', 'red'), data.error);
    }
  } catch (error) {
    console.log(colorize('❌ Error:', 'red'), error.message);
  }
}

// Handle languages command
async function handleLanguages() {
  try {
    const response = await makeRequest('/languages');
    const { data } = response;
    
    console.log(colorize('\n📚 Supported Languages:', 'bright'));
    data.languages.forEach(lang => {
      console.log(colorize(`  ${lang.code.toUpperCase()}`, 'green') + ` - ${lang.name} (${lang.nativeName})`);
    });
    
    console.log(colorize('\n🔄 Translation Pairs:', 'bright'));
    data.pairs.forEach(pair => {
      console.log(colorize(`  ${pair}`, 'cyan'));
    });
    console.log('');
  } catch (error) {
    console.log(colorize('❌ Error:', 'red'), error.message);
  }
}

// Handle download command
async function handleDownload(lang, target = 'en') {
  try {
    console.log(colorize(`\n⏳ Downloading ${lang}-${target} language pack...`, 'yellow'));
    
    const response = await makeRequest(`/download/${lang}?target=${target}`);
    const { data } = response;
    
    if (data.error) {
      console.log(colorize('❌ Error:', 'red'), data.message);
      return;
    }
    
    console.log(colorize('\n✓ Download Successful:', 'green'));
    console.log(colorize('  Pack:    ', 'cyan') + data.pack);
    console.log(colorize('  Words:   ', 'cyan') + data.words);
    console.log(colorize('  Version: ', 'cyan') + data.version);
    console.log(colorize('  Time:    ', 'cyan') + new Date(data.downloadedAt).toLocaleString());
    console.log(colorize('\n  Sample words:', 'yellow'));
    
    const words = Object.entries(data.dictionary).slice(0, 10);
    words.forEach(([en, tr]) => {
      console.log(`    ${en.padEnd(20)} → ${tr}`);
    });
    console.log(colorize(`    ... and ${data.words - 10} more\n`, 'yellow'));
  } catch (error) {
    console.log(colorize('❌ Error:', 'red'), error.message);
  }
}

// Handle health check
async function handleHealth() {
  try {
    const response = await makeRequest('/');
    const { data } = response;
    
    console.log(colorize('\n💚 Server Health:', 'green'));
    console.log(colorize('  Service:   ', 'cyan') + data.service);
    console.log(colorize('  Version:   ', 'cyan') + data.version);
    console.log(colorize('  Status:    ', 'cyan') + colorize(data.status.toUpperCase(), 'green'));
    console.log(colorize('  Security:  ', 'cyan') + data.security);
    console.log(colorize('  Languages: ', 'cyan') + data.languages.join(', '));
    console.log(colorize('  Time:      ', 'cyan') + new Date(data.timestamp).toLocaleString());
    console.log('');
  } catch (error) {
    console.log(colorize('❌ Server Offline:', 'red'), error.message);
  }
}

// Main REPL
async function main() {
  showBanner();
  showHelp();
  
  // Check server health on startup
  await handleHealth();
  
  function prompt() {
    rl.question(colorize('translation> ', 'magenta'), async (input) => {
      const parts = input.trim().split(/\s+/);
      const command = parts[0].toLowerCase();
      
      switch (command) {
        case 'translate':
          if (parts.length < 4) {
            console.log(colorize('Usage: translate <text> <from> <to>', 'yellow'));
          } else {
            const to = parts.pop();
            const from = parts.pop();
            const text = parts.slice(1).join(' ').replace(/['"]/g, '');
            await handleTranslate(text, from, to);
          }
          break;
          
        case 'languages':
        case 'langs':
          await handleLanguages();
          break;
          
        case 'download':
        case 'dl':
          if (parts.length < 2) {
            console.log(colorize('Usage: download <lang> [target]', 'yellow'));
          } else {
            await handleDownload(parts[1], parts[2]);
          }
          break;
          
        case 'test':
          if (parts.length < 4) {
            console.log(colorize('Usage: test <text> <from> <to>', 'yellow'));
          } else {
            const to = parts.pop();
            const from = parts.pop();
            const text = parts.slice(1).join(' ').replace(/['"]/g, '');
            await handleTranslate(text, from, to);
          }
          break;
          
        case 'health':
        case 'status':
          await handleHealth();
          break;
          
        case 'help':
        case '?':
          showHelp();
          break;
          
        case 'exit':
        case 'quit':
        case 'q':
          console.log(colorize('\n👋 Goodbye!\n', 'cyan'));
          rl.close();
          process.exit(0);
          break;
          
        case '':
          break;
          
        default:
          console.log(colorize(`Unknown command: ${command}`, 'red'));
          console.log(colorize('Type "help" for available commands\n', 'yellow'));
      }
      
      if (command !== 'exit' && command !== 'quit' && command !== 'q') {
        prompt();
      }
    });
  }
  
  prompt();
}

// Handle Ctrl+C
rl.on('SIGINT', () => {
  console.log(colorize('\n\n👋 Goodbye!\n', 'cyan'));
  process.exit(0);
});

// Start CLI
if (require.main === module) {
  main().catch(console.error);
}

module.exports = { makeRequest, signRequest, verifyResponse };
