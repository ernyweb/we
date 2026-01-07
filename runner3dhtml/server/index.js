require('dotenv').config();
const express = require('express');
const helmet = require('helmet');
const cors = require('cors');
const cookieParser = require('cookie-parser');
const bcrypt = require('bcrypt');
const jwt = require('jsonwebtoken');
const db = require('./db');

const JWT_SECRET = process.env.JWT_SECRET || 'dev-secret-change-me';
const PORT = process.env.PORT || 4000;
const ALLOWED_ORIGIN = process.env.ALLOWED_ORIGIN || '*';

const app = express();
app.use(helmet());
app.use(express.json());
app.use(cookieParser());
app.use(cors({ origin: ALLOWED_ORIGIN === '*' ? true : ALLOWED_ORIGIN, credentials: true }));

// Init DB
db.init();

function authMiddleware(req, res, next){
  const token = req.cookies?.token || (req.headers.authorization ? req.headers.authorization.split(' ')[1] : null);
  if(!token) return res.status(401).json({status:'error', error:'unauthenticated'});
  try{
    const p = jwt.verify(token, JWT_SECRET);
    const user = db.getUserById(p.id);
    if(!user) return res.status(401).json({status:'error', error:'unauthenticated'});
    req.user = user;
    next();
  }catch(e){ console.warn('auth failed', e); return res.status(401).json({status:'error', error:'unauthenticated'}); }
}

app.post('/api/register', async (req,res)=>{
  try{
    const {username, password} = req.body || {};
    if(!username || !password || username.length < 2 || password.length < 4) return res.status(400).json({status:'error', error:'invalid'});
    const existed = db.getUserByName(username);
    if(existed) return res.status(409).json({status:'error', error:'exists'});
    const hash = await bcrypt.hash(password, 12);
    const u = db.createUser(username, hash);
    return res.json({status:'ok', user:u});
  }catch(e){ console.error(e); return res.status(500).json({status:'error', error:'server'}); }
});

app.post('/api/login', async (req,res)=>{
  try{
    const {username, password} = req.body || {};
    const u = db.getUserByName(username);
    if(!u) return res.status(404).json({status:'error', error:'no'});
    const ok = await bcrypt.compare(password, u.password);
    if(!ok) return res.status(403).json({status:'error', error:'bad'});
    const token = jwt.sign({id:u.id, username:u.username}, JWT_SECRET, {expiresIn:'7d'});
    const secure = process.env.NODE_ENV === 'production';
    res.cookie('token', token, {httpOnly:true, secure, sameSite:'lax', maxAge:7*24*3600*1000});
    const me = db.getUserById(u.id);
    return res.json({status:'ok', user: me});
  }catch(e){ console.error(e); return res.status(500).json({status:'error', error:'server'}); }
});

app.post('/api/score', authMiddleware, async (req,res)=>{
  try{
    const {score} = req.body || {};
    if(typeof score !== 'number') return res.status(400).json({status:'error', error:'invalid'});
    const best = db.addScore(req.user.id, score);
    return res.json({status:'ok', best});
  }catch(e){ console.error(e); return res.status(500).json({status:'error', error:'server'}); }
});

app.get('/api/leaderboard', (req,res)=>{
  try{
    const top = db.topPlayers(20);
    return res.json({status:'ok', top});
  }catch(e){ console.error(e); return res.status(500).json({status:'error', error:'server'}); }
});

app.get('/api/me', authMiddleware, (req,res)=>{
  return res.json({status:'ok', user: req.user});
});

app.post('/api/logout', (req,res)=>{
  // clear the auth cookie
  res.cookie('token', '', { httpOnly: true, secure: process.env.NODE_ENV === 'production', sameSite: 'lax', expires: new Date(0) });
  return res.json({status:'ok'});
});

app.listen(PORT, ()=> console.log('Runner3D server listening on', PORT));
