const db = require('./db');

console.log('Initializing DB...');
try{
  db.init();
  console.log('DB initialized.');
  console.log('You can run `node init_db.js` to re-run this initialization.');
}catch(e){ console.error('DB init failed', e); }
