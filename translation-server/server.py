#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Translation Server - Python Flask
Simple API key authentication, no signatures
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import hashlib
import json
import os
from datetime import datetime

app = Flask(__name__)
CORS(app)

# API Key Hashes (SHA256)
# mobile-internal-audio-key-2026-xyz789 -> hash
VALID_API_KEY_HASHES = {
    hashlib.sha256('mobile-internal-audio-key-2026-xyz789'.encode()).hexdigest(),
    hashlib.sha256('translation-key-2026-secure-abc123'.encode()).hexdigest(),
    hashlib.sha256('mobile-app-key-xyz789'.encode()).hexdigest(),
}

# Request counter
request_count = 0

def log(emoji, message):
    """Pretty console logging"""
    timestamp = datetime.now().strftime('%H:%M:%S')
    print(f"[{timestamp}] {emoji} {message}")

def load_dictionaries():
    """Load translation dictionaries"""
    dict_dir = os.path.join(os.path.dirname(__file__), 'lang')
    dictionaries = {}
    
    files = [
        'en-tr.json', 'tr-en.json',
        'en-ru.json', 'ru-en.json',
        'en-es.json', 'es-en.json',
        'en-fr.json', 'fr-en.json'
    ]
    
    for filename in files:
        path = os.path.join(dict_dir, filename)
        if os.path.exists(path):
            with open(path, 'r', encoding='utf-8') as f:
                key = filename.replace('.json', '')
                dictionaries[key] = json.load(f)
                log('📚', f'Loaded {key}: {len(dictionaries[key])} words')
    
    return dictionaries

DICTIONARIES = load_dictionaries()

def verify_api_key():
    """Check if API key is valid (hashed)"""
    api_key = request.headers.get('X-API-Key') or request.args.get('api_key')
    
    if not api_key:
        return False
    
    key_hash = hashlib.sha256(api_key.encode()).hexdigest()
    return key_hash in VALID_API_KEY_HASHES

def translate(text, source, target):
    """Translate text using dictionary"""
    key = f"{source.lower()}-{target.lower()}"
    dictionary = DICTIONARIES.get(key)
    
    if not dictionary:
        return None
    
    # Split and translate
    words = text.lower().strip().split()
    translated_words = []
    translated_count = 0
    
    for word in words:
        clean_word = word.strip('.,!?;:')
        if clean_word in dictionary:
            translated_words.append(dictionary[clean_word])
            translated_count += 1
        else:
            translated_words.append(word)
    
    return {
        'success': True,
        'original': text,
        'translated': ' '.join(translated_words),
        'from': source.upper(),
        'to': target.upper(),
        'words_translated': translated_count,
        'total_words': len(words)
    }

@app.before_request
def before_request():
    """Log incoming requests"""
    global request_count
    request_count += 1
    
    ip = request.remote_addr
    method = request.method
    path = request.path
    
    log('📨', f'{method} {path} from {ip}')

@app.route('/', methods=['GET'])
def home():
    """Health check"""
    log('✅', 'Health check')
    return jsonify({
        'service': 'Translation Server',
        'version': '3.0.0',
        'status': 'online',
        'engine': 'Python Flask',
        'languages': ['EN', 'TR', 'RU', 'ES', 'FR'],
        'requests': request_count
    })

@app.route('/translate', methods=['POST'])
def translate_endpoint():
    """Translation endpoint"""
    # Verify API key
    if not verify_api_key():
        log('❌', 'Invalid API key')
        return jsonify({'error': 'Invalid API key'}), 401
    
    # Get data
    data = request.get_json()
    if not data:
        log('⚠️', 'No JSON data')
        return jsonify({'error': 'No JSON data'}), 400
    
    text = data.get('text', '')
    source = (data.get('source') or data.get('from', 'EN')).upper()
    target = (data.get('target') or data.get('to', 'TR')).upper()
    
    if not text:
        log('⚠️', 'Missing text field')
        return jsonify({'error': 'Missing text'}), 400
    
    # Translate
    result = translate(text, source, target)
    
    if result:
        log('✅', f'Translated: "{text}" ({source}→{target}) = "{result["translated"]}"')
        return jsonify(result)
    else:
        log('❌', f'Unsupported pair: {source}-{target}')
        return jsonify({
            'success': False,
            'error': f'Translation pair {source}-{target} not supported'
        }), 400

@app.route('/languages', methods=['GET'])
def languages():
    """List supported languages"""
    if not verify_api_key():
        return jsonify({'error': 'Invalid API key'}), 401
    
    return jsonify({
        'languages': [
            {'code': 'EN', 'name': 'English'},
            {'code': 'TR', 'name': 'Turkish'},
            {'code': 'RU', 'name': 'Russian'},
            {'code': 'ES', 'name': 'Spanish'},
            {'code': 'FR', 'name': 'French'}
        ],
        'pairs': list(DICTIONARIES.keys())
    })

if __name__ == '__main__':
    print("\n" + "="*60)
    print("  🐍 TRANSLATION SERVER - PYTHON FLASK")
    print("="*60)
    print(f"  Status: ✅ ONLINE")
    print(f"  Port: 80")
    print(f"  Security: API Key (SHA256 Hash)")
    print(f"  Languages: EN, TR, RU, ES, FR")
    print(f"  Dictionaries: {len(DICTIONARIES)}")
    print(f"  Total words: {sum(len(d) for d in DICTIONARIES.values())}")
    print("="*60)
    print(f"  🌐 Server: http://0.0.0.0:80")
    print(f"  🔑 API Keys: {len(VALID_API_KEY_HASHES)} configured")
    print("="*60 + "\n")
    
    app.run(host='0.0.0.0', port=80, debug=False)
