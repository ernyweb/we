Runner3D server
=================

Minimal express server to store users and scores securely on a VPS using SQLite and bcrypt.

Features
- HTTPS-ready (use a reverse proxy or run behind nginx)
- Passwords hashed with bcrypt
- JWT based authentication (cookie, HttpOnly)
- Score submission and leaderboard

Quick start (VPS)
1. Install Node.js 18+ and build tools
2. git clone your repo and cd into `/server`
3. copy `.env.example` -> `.env` and set `JWT_SECRET` to a strong value and `ALLOWED_ORIGIN` to your frontend origin
4. npm install
5. npm run init-db
6. Start server: `npm start` (or use `pm2 start index.js --name runner3d` or a systemd service)

Security notes
- Use HTTPS in production and run the server behind a reverse proxy (nginx) that handles TLS.
- Keep `JWT_SECRET` secret; rotate it if you suspect a leak.
- Set `ALLOWED_ORIGIN` to the exact origin of your website rather than `*` in production.

API endpoints
- POST /api/register {username, password} -> {status:'ok', user} or {status:'error', error}
- POST /api/login {username, password} -> {status:'ok', user} (server sets HttpOnly cookie)
- POST /api/score {score} (requires auth cookie) -> {status:'ok', best}
- GET /api/leaderboard -> {status:'ok', top: [{username, best}]}
- GET /api/me (requires auth) -> {status:'ok', user}
- POST /api/logout -> {status:'ok'} (clears the auth cookie on the server)

Notes:
- If the frontend sets `window.API_BASE`, guest accounts will be automatically created on the server when users continue as guest.

Frontend integration
- Set `window.API_BASE = 'https://your-vps.example.com'` in `index.html` before loading `game.js` or set a small inline script to set the variable.
- Frontend should use credentials: 'include' on fetch calls so the cookie is sent.

Example cURL
- Register: curl -X POST -H "Content-Type: application/json" -d '{"username":"joe","password":"pw"}' https://your-vps/api/register
- Login: curl -X POST -c cookies.txt -H "Content-Type: application/json" -d '{"username":"joe","password":"pw"}' https://your-vps/api/login
- Post score: curl -X POST -b cookies.txt -H "Content-Type: application/json" -d '{"score":123}' https://your-vps/api/score

