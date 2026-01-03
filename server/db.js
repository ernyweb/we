const Database = require('better-sqlite3');
const path = require('path');
const dbPath = process.env.DB_PATH || path.join(__dirname, 'runner3d.db');
const db = new Database(dbPath);

function init(){
  db.pragma('journal_mode = WAL');
  db.prepare(`CREATE TABLE IF NOT EXISTS users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT UNIQUE NOT NULL,
    password TEXT NOT NULL,
    best INTEGER DEFAULT 0,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP
  )`).run();

  db.prepare(`CREATE TABLE IF NOT EXISTS scores (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    score INTEGER NOT NULL,
    created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY(user_id) REFERENCES users(id)
  )`).run();
}

function getUserByName(username){
  return db.prepare('SELECT id, username, password, best FROM users WHERE username = ?').get(username);
}

function createUser(username, passwordHash){
  const stmt = db.prepare('INSERT INTO users (username, password) VALUES (?, ?)');
  const info = stmt.run(username, passwordHash);
  return getUserById(info.lastInsertRowid);
}

function getUserById(id){
  return db.prepare('SELECT id, username, best FROM users WHERE id = ?').get(id);
}

function addScore(userId, score){
  const stmt = db.prepare('INSERT INTO scores (user_id, score) VALUES (?, ?)');
  stmt.run(userId, Math.floor(score));
  // update best if necessary
  const cur = db.prepare('SELECT best FROM users WHERE id = ?').get(userId);
  if(!cur || score > cur.best){
    db.prepare('UPDATE users SET best = ? WHERE id = ?').run(Math.floor(score), userId);
    return Math.floor(score);
  }
  return cur.best;
}

function topPlayers(limit = 10){
  return db.prepare('SELECT username, best FROM users WHERE best > 0 ORDER BY best DESC, created_at ASC LIMIT ?').all(limit);
}

module.exports = { init, getUserByName, createUser, getUserById, addScore, topPlayers, db };
