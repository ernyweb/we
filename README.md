# webgame

This project contains Runner3D — a small browser game. A minimal secure backend is included in `/server` for storing user accounts and persistent leaderboards on a VPS.

Server quickstart:
- cd server
- copy `.env.example` to `.env` and set `JWT_SECRET` and `ALLOWED_ORIGIN`
- npm install
- npm run init-db
- npm start

Set `window.API_BASE` in `index.html` (or an inline script) to point to the server URL (e.g. `http://localhost:4000` for local testing or `https://your-vps.example.com` for production).