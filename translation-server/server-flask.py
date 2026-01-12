#!/usr/bin/env python3
"""
Simple Translation Server - Flask
Port 80, API Key authentication
"""

from flask import Flask, request, jsonify
from flask_cors import CORS
import json
import os

app = Flask(__name__)
CORS(app)

# API Keys
VALID_API_KEYS = {
    'translation-key-2026-secure-abc123',
    'mobile-app-key-xyz789',
    'mobile-internal-audio-key-2026-xyz789',
}

# Load dictionaries
DICT_DIR = os.path.join(os.path.dirname(__file__), 'lang')

def load_dict(filename):
    path = os.path.join(DICT_DIR, filename)
    if os.path.exists(path):
        with open(path, 'r', encoding='utf-8') as f:
            return json.load(f)
    return {}

DICTIONARIES = {
    'en-tr': load_dict('en-tr.json'),
    'tr-en': load_dict('tr-en.json'),
    'en-ru': load_dict('en-ru.json'),
    'ru-en': load_dict('ru-en.json'),
    'en-es': load_dict('en-es.json'),
    'es-en': load_dict('es-en.json'),
    'en-fr': load_dict('en-fr.json'),
    'fr-en': load_dict('fr-en.json'),
}

def check_api_key():
    """Check if API key is valid"""
    api_key = request.headers.get('X-API-Key') or request.args.get('api_key')
    return api_key in VALID_API_KEYS

def translate_text(text, source, target):
    """Translate text using dictionary"""
    key = f"{source.lower()}-{target.lower()}"
    dictionary = DICTIONARIES.get(key)
    
    if not dictionary:
        return None
    
    words = text.lower().strip().split()
    translated = [dictionary.get(word, word) for word in words]
    
    return {
        'success': True,
        'original': text,
        'translated': ' '.join(translated),
        'from': source,
        'to': target,
        'wordsTranslated': sum(1 for w, t in zip(words, translated) if dictionary.get(w)),
        'totalWords': len(words)
    }

@app.route('/', methods=['GET'])
def home():
    """Health check"""
    return jsonify({
        'service': 'Translation Server',
        'version': '3.0.0',
        'status': 'online',
        'languages': ['en', 'tr', 'ru', 'es', 'fr'],
        'engine': 'Python Flask'
    })

@app.route('/translate', methods=['POST'])
def translate():
    """Translate endpoint"""
    # Check API key
    if not check_api_key():
        return jsonify({'error': 'Invalid API key'}), 401
    
    # Get request data
    data = request.get_json()
    if not data:
        return jsonify({'error': 'No JSON data'}), 400
    
    text = data.get('text')
    source = data.get('source') or data.get('from', 'EN')
    target = data.get('target') or data.get('to', 'TR')
    
    if not text:
        return jsonify({'error': 'Missing text field'}), 400
    
    # Translate
    result = translate_text(text, source, target)
    
    if result:
        return jsonify({'data': result})
    else:
        return jsonify({
            'data': {
                'success': False,
                'error': f'Translation pair {source}-{target} not supported'
            }
        })

@app.route('/languages', methods=['GET'])
def languages():
    """List available languages"""
    if not check_api_key():
        return jsonify({'error': 'Invalid API key'}), 401
    
    return jsonify({
        'languages': [
            {'code': 'en', 'name': 'English'},
            {'code': 'tr', 'name': 'Turkish'},
            {'code': 'ru', 'name': 'Russian'},
            {'code': 'es', 'name': 'Spanish'},
            {'code': 'fr', 'name': 'French'}
        ],
        'pairs': list(DICTIONARIES.keys())
    })

if __name__ == '__main__':
    print("=" * 60)
    print("🐍 Translation Server - Python Flask")
    print("=" * 60)
    print(f"Status: ONLINE ✓")
    print(f"Port: 80")
    print(f"Security: API Key Authentication")
    print(f"Languages: EN, TR, RU, ES, FR")
    print(f"Dictionaries: {len(DICTIONARIES)}")
    print("=" * 60)
    print(f"Server ready at http://0.0.0.0:80")
    print(f"API Keys configured: {len(VALID_API_KEYS)}")
    print("=" * 60)
    
    app.run(host='0.0.0.0', port=80, debug=False)
